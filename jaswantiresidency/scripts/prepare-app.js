// Prepares the bundled fallback pages for the Android app: writes the live site's address (APP_URL) into the
// offline screen's "Try again" button. Run after `npm run build`, before `cap sync` (see the app:sync script).
const fs = require('fs');
const path = require('path');

const appUrl = process.env.APP_URL;
if (!appUrl || !/^https:\/\/[^\s'"<>]+$/.test(appUrl)) {
    console.error('Set APP_URL to the live site, e.g. APP_URL=https://app.example.in npm run app:sync');
    process.exit(1);
}

const file = path.join(__dirname, '..', 'build', 'offline.html');
if (!fs.existsSync(file)) {
    console.error('build/offline.html not found: run `npm run build` first');
    process.exit(1);
}
fs.writeFileSync(file, fs.readFileSync(file, 'utf8').replace('__APP_URL__', appUrl.replace(/\/$/, '')));
console.log(`Offline screen will retry ${appUrl}`);
