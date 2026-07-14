FROM node:22-bookworm

WORKDIR /app

# Install deps without downloading all Playwright browsers during npm install
COPY package.json package-lock.json* ./
RUN PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install

# Download Chromium AND its system libraries (runs as root in Docker build)
RUN npx playwright install --with-deps chromium

COPY . .

ENV PORT=3000
EXPOSE 3000

CMD ["node", "server.js"]
