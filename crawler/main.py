import time

from crawler.student_meal import run_student_meal
from crawler.faculty_meal import run_faculty_meal
from crawler.dorm_meal import run_dorm_meal
from crawler.scholarship_notice import run_scholarship_notice
from crawler.academic_notice import run_academic_notice
from crawler.hankyong_notice import run_hankyong_notice
from crawler.academic_schedule import run_academic_schedule

def main():
    jobs = [
        ("학생식당", run_student_meal),
        ("교직원식당", run_faculty_meal),
        ("기숙사식당", run_dorm_meal),
        ("장학공지", run_scholarship_notice),
        ("학사공지", run_academic_notice),
        ("한경공지", run_hankyong_notice),
        ("학사일정", run_academic_schedule)
    ]

    for name, job in jobs:
        try:
            print(f"\n✅ [{name}] 실행 시작")
            job()
            time.sleep(5)
        except Exception as e:
            print(f"❌ [{name}] 오류 발생: {e}")

if __name__ == "__main__":
    main()