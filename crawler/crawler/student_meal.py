import requests
from bs4 import BeautifulSoup
import urllib3
import pymysql
import hashlib
import re
import os
from dotenv import load_dotenv
from collections import defaultdict

# .env 로드
load_dotenv(dotenv_path="/root/hknu_scraper/.env")

# SSL 경고 무시
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def clean_date(date_str):
    match = re.match(r"(\d{4})\.(\d{2})\.(\d{2})", date_str)
    if match:
        return f"{match.group(1)}-{match.group(2)}-{match.group(3)}"
    return date_str.strip()

def make_hash(date, time, menu):
    return hashlib.sha256((date + time + menu).encode("utf-8")).hexdigest()

def run_student_meal():
    print("🍽 한경국립대학교 학생식당 식단표")
    print("=" * 30)

    url = "https://www.hknu.ac.kr/kor/670/subview.do"
    req = requests.get(url, verify=False)
    soup = BeautifulSoup(req.text, "html.parser")

    grouped_meals = defaultdict(list)
    tables = soup.find_all("table")
    current_date = ""

    for table in tables:
        rows = table.find_all("tr")
        for row in rows:
            date_cell = row.find("th", class_="dietDate")
            if date_cell:
                current_date = clean_date(date_cell.get_text(strip=True))

            time_cell = row.find("td", class_="dietNm")
            content_cell = row.find("td", class_="dietCont")

            if current_date and time_cell and content_cell:
                time_text = time_cell.get_text(strip=True)
                menu_items = content_cell.get_text(separator="\n", strip=True).split("\n")
                menu_text = "\n".join(item.lstrip("- ").strip() for item in menu_items if item.strip())
                hash_val = make_hash(current_date, time_text, menu_text)

                grouped_meals[current_date].append({
                    "time": time_text,
                    "menu": menu_text,
                    "hash": hash_val
                })

    # ✅ .env 기반 DB 연결
    conn = pymysql.connect(
        host=os.getenv("DB_HOST"),
        port=int(os.getenv("DB_PORT")),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
        charset="utf8mb4"
    )
    cursor = conn.cursor()
    cursor.execute("SELECT hash FROM student_meals")
    existing_hashes = {row[0] for row in cursor.fetchall()}

    sql = """
    INSERT INTO student_meals (meal_date, meal_time, menu, hash)
    SELECT %s, %s, %s, %s FROM DUAL
    WHERE NOT EXISTS (
        SELECT 1 FROM student_meals WHERE hash = %s
    )
    """

    inserted_count = 0
    for date, meals in grouped_meals.items():
        daily_inserted = 0
        for meal in meals:
            if meal["hash"] in existing_hashes:
                continue

            if daily_inserted == 0:
                print(f"📅 날짜: {date}")
            print(f"[{meal['time']}]")
            print(meal["menu"])
            print("-" * 30)

            cursor.execute(sql, (
                date,
                meal["time"],
                meal["menu"],
                meal["hash"],
                meal["hash"]
            ))
            inserted_count += 1
            daily_inserted += 1

    conn.commit()
    cursor.close()
    conn.close()

    if inserted_count:
        print(f"✅ {inserted_count}건의 새 식단을 저장했습니다.")
    else:
        print("✅ 새로운 식단이 없습니다.")