import urllib3
import requests
from bs4 import BeautifulSoup
import pymysql
import hashlib
from datetime import datetime
import re
import os
from dotenv import load_dotenv

# .env 로드
load_dotenv(dotenv_path="/root/hknu_scraper/.env")

def clean_menu_text(text):
    text = text.strip()
    text = re.sub(r"^-+", "", text)
    text = text.replace("(통합)", "")
    return text.strip()

def clean_trailing_symbols(text):
    return re.sub(r'[\"*]+$', '', text).strip()

def format_meal(day: str, breakfast: str, lunch: str, dinner: str) -> str:
    def extract_time_and_menu(meal_text: str):
        match = re.match(r"(\d{1,2}:\d{2}~\d{1,2}:\d{2})(.*)", meal_text.strip())
        if match:
            return match.group(1), match.group(2)
        else:
            return "", meal_text.strip()

    # 식단 없음 (공휴일 등) 처리
    only_msg = breakfast + lunch + dinner
    if only_msg.strip() and not any(meal_kw in only_msg for meal_kw in ["밥", "국", "김치", "샐러드", "삼각김밥", "샌드위치", "김밥"]):
        return only_msg.strip()

    result = ""

    if breakfast.strip():
        result += f"[아침] {clean_trailing_symbols(breakfast.strip())}\n"
    if lunch.strip():
        lunch_time, lunch_menu = extract_time_and_menu(lunch)
        result += f"[점심]{f' {lunch_time}' if lunch_time else ''}\n{clean_trailing_symbols(lunch_menu)}\n"
    if dinner.strip():
        dinner_time, dinner_menu = extract_time_and_menu(dinner)
        result += f"[저녁]{f' {dinner_time}' if dinner_time else ''}\n{clean_trailing_symbols(dinner_menu)}\n"

    return result.strip()

def run_dorm_meal():
    print("🍽️ 한경국립대학교 기숙사식당 식단표")
    print("=" * 30)

    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    url = "https://dormitory.hknu.ac.kr/bbs/board.php?bo_table=foodplan&h_flag=2"
    response = requests.get(url, verify=False)
    response.encoding = 'utf-8'

    soup = BeautifulSoup(response.text, "html.parser")
    table = soup.find("table", class_="zp_schedule")
    meals = []

    if table:
        rows = table.find_all("tr")
        for row in rows:
            tds = row.find_all("td")
            for td in tds:
                date_tag = td.find("span", class_="caldate")
                menu_ul = td.find("ul", class_="plan")

                if date_tag and menu_ul:
                    raw_date = date_tag.get_text(strip=True)[:10]
                    try:
                        date_obj = datetime.strptime(raw_date, "%Y.%m.%d")
                        date_formatted = date_obj.strftime("%Y-%m-%d")
                    except ValueError:
                        date_formatted = f"2025-06-{raw_date.zfill(2)}"

                    menu_items = [
                        clean_menu_text(li.get_text())
                        for li in menu_ul.find_all("li")
                        if li.get_text(strip=True)
                    ]

                    breakfast = menu_items[0] if len(menu_items) > 0 else ""
                    lunch = menu_items[1] if len(menu_items) > 1 else ""
                    dinner = menu_items[2] if len(menu_items) > 2 else ""

                    formatted_menu = format_meal(date_formatted, breakfast, lunch, dinner)
                    hash_val = hashlib.sha256((date_formatted + formatted_menu).encode("utf-8")).hexdigest()

                    meals.append({
                        "date": date_formatted,
                        "menu": formatted_menu,
                        "hash": hash_val
                    })

    # DB 연결 및 저장
    conn = pymysql.connect(
        host=os.getenv("DB_HOST"),
        port=int(os.getenv("DB_PORT")),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
        charset="utf8mb4"
    )

    new_meals = []
    with conn.cursor() as cursor:
        cursor.execute("SELECT hash FROM dorm_meals")
        existing_hashes = {row[0] for row in cursor.fetchall()}

        sql = """
        INSERT INTO dorm_meals (meal_date, meal_time, menu, hash)
        SELECT %s, %s, %s, %s FROM DUAL
        WHERE NOT EXISTS (
            SELECT 1 FROM dorm_meals WHERE hash = %s
        )
        """

        for meal in meals:
            if meal["hash"] in existing_hashes:
                continue
            new_meals.append(meal)
            cursor.execute(sql, (
                meal["date"], "", meal["menu"],
                meal["hash"], meal["hash"]
            ))

    conn.commit()
    conn.close()

    # 결과 출력
    if new_meals:
        for meal in new_meals:
            print(f"📅 날짜: {meal['date']}")
            print("🍽️ 메뉴:")
            print(meal['menu'])
            print("-" * 30)
        print(f"✅ {len(new_meals)}건의 새 식단을 저장했습니다.")
    else:
        print("✅ 새로운 식단이 없습니다.")