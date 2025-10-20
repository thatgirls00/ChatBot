import requests
import urllib3
from bs4 import BeautifulSoup
import pymysql
import re
import hashlib
import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv(dotenv_path="/root/hknu_scraper/.env")

# SSL 경고 무시
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def normalize_dates(raw, last_month="01"):
    try:
        dates = re.findall(r"(\d{1,2})\.(\d{1,2})", raw)
        if len(dates) == 2:
            sm, sd = dates[0]
            em, ed = dates[1]
        elif len(dates) == 1:
            em, ed = dates[0]
            sm, sd = last_month, ed
        else:
            return "2025-01-01", "2025-01-01", last_month

        sm = sm.zfill(2)
        sd = sd.zfill(2)
        em = em.zfill(2)
        ed = ed.zfill(2)

        start_date = f"2025-{sm}-{sd}"
        end_year = "2025" if int(sm) <= int(em) else "2026"
        end_date = f"{end_year}-{em}-{ed}"

        return start_date, end_date, sm
    except:
        return "2025-01-01", "2025-01-01", last_month

def generate_hash(start_date, content):
    return hashlib.sha256(f"{start_date}_{content}".encode("utf-8")).hexdigest()

def run_academic_schedule():
    print("📅 한경국립대학교 학사일정")
    print("=" * 30)

    url = "https://www.hknu.ac.kr/kor/646/subview.do"
    response = requests.get(url, verify=False)
    response.encoding = 'utf-8'
    soup = BeautifulSoup(response.text, "html.parser")

    schedule_div = soup.find("div", id="schdulWrap")
    schedule_items = []

    # .env에서 불러오기
    conn = pymysql.connect(
        host=os.getenv("DB_HOST"),
        port=int(os.getenv("DB_PORT")),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
        charset="utf8mb4"
    )
    cursor = conn.cursor()

    cursor.execute("SELECT hash FROM academic_schedule")
    existing_hashes = set(row[0] for row in cursor.fetchall())

    if schedule_div:
        items = schedule_div.find_all("li")
        last_month = "01"

        for item in items:
            raw_text = item.get_text(separator=" ", strip=True)
            clean_text = re.sub(r"\s+", " ", raw_text)

            match = re.match(r"([\d\.\~\(\)\s\-]+)\s+(.*)", clean_text)
            if not match:
                print(f"❗ 구문 분석 실패: {clean_text}")
                continue

            raw_date = match.group(1).strip()
            all_contents = match.group(2).strip()

            entries = re.split(r'(?=\d{2}\.\d{2}\s*\()', all_contents)
            for entry in entries:
                content = entry.strip()
                if not content:
                    continue

                start_date, _, last_month = normalize_dates(content, last_month)
                hash_val = generate_hash(start_date, content)

                if hash_val in existing_hashes:
                    continue

                print(content)
                schedule_items.append((start_date, content, hash_val))
    else:
        print("❗ 학사일정 데이터를 찾을 수 없습니다.")

    if schedule_items:
        sql = """
        INSERT INTO academic_schedule (date, content, hash)
        VALUES (%s, %s, %s)
        """
        cursor.executemany(sql, schedule_items)
        conn.commit()
        print(f"✅ {len(schedule_items)}건의 새 일정을 저장했습니다.")
    else:
        print("✅ 새로운 일정이 없습니다.")

    cursor.close()
    conn.close()