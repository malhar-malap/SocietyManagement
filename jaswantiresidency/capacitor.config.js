// Android app shell. The app loads the hosted website (APP_URL), so every deploy reaches installed apps without
// reinstalling. Build the APK with APP_URL set to the live site, e.g.  APP_URL=https://app.example.in npx cap sync android
const appUrl = process.env.APP_URL || 'https://app.example.invalid';

if (!/^https:\/\//.test(appUrl)) {
    throw new Error('APP_URL must be an https:// address: the app never talks to the server over plain HTTP');
}

module.exports = {
    appId: 'in.jaswantiresidency.app',
    appName: 'Jaswanti Residency',
    // Bundled fallback pages (offline screen); the live screens come from server.url
    webDir: 'build',
    server: {
        url: appUrl,
        cleartext: false,
        androidScheme: 'https',
        // Shown when the site cannot be reached (no internet, server down)
        errorPath: 'offline.html',
    },
    android: {
        allowMixedContent: false,
        webContentsDebuggingEnabled: false,
    },
};
