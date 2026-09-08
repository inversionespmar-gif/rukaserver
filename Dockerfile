FROM node:22-slim

WORKDIR /app

# Install deps without downloading all Playwright browsers during npm install
COPY package.json package-lock.json* ./
RUN PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install

# Download Chromium with its system dependencies
RUN npx playwright install --with-deps chromium

COPY . .

ENV PORT=3000
ENV NODE_OPTIONS=--max-old-space-size=350
EXPOSE 3000

CMD ["node", "server.js"]
