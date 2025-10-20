FROM python:3.11

# 시스템 필수 패키지 설치 (chromium 및 chromedriver 포함)
RUN apt-get update && apt-get install -y \
    wget curl unzip gnupg \
    libglib2.0-0 libnss3 libgconf-2-4 libxss1 libasound2 \
    libatk1.0-0 libatk-bridge2.0-0 libcups2 \
    fonts-liberation libappindicator3-1 xdg-utils \
    chromium chromium-driver \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리
WORKDIR /app

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY . .

# 실행 명령
CMD ["python", "main.py"]