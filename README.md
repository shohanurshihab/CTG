# CTG TV - Android TV app for media.ctgfun.com

A Netflix-style front end for the nginx directory server at https://media.ctgfun.com/.
It reads the folder pages, shows movie folders as poster cards (posters and descriptions from TMDB),
and plays the video files directly with ExoPlayer, with resume and side-loaded subtitles.

## Get the APK (no Android Studio needed)

1. Create a free account at https://github.com and make a **new repository** (private is fine).
2. Upload everything from this folder into it, **including the hidden `.github` folder**
   (on the repo page: Add file > Upload files, then drag the whole folder contents in).
   If `.github/workflows/build.yml` did not upload, use Add file > Create new file, type
   `.github/workflows/build.yml` as the name and paste the contents of that file.
3. (Optional) Add your TMDB key so posters work out of the box:
   Settings > Secrets and variables > Actions > New repository secret, name `TMDB_API_KEY`.
   You can also type it in the app later under Settings.
4. Open the **Actions** tab. The "Build APK" workflow runs automatically (about 5 minutes).
   If it does not start, open it and press "Run workflow".
5. When it is green, open the run and download the **CtgTv-APK** artifact (a zip containing `app-debug.apk`).
6. Copy the APK to the TV (USB stick, or the "Send Files to TV" / "Downloader" app) and install it.
   You may need to allow "Install unknown apps" for the file manager you use.

If the build fails, open the failed step, copy the red error lines and send them to Claude.

## Using it

- **Home** shows one row per folder you added. The default row is
  `disk1/Recent English Movies/2022/`.
- **Browse all** walks the whole server (disk1 ... disk11). Open any folder full of movies and choose
  **Add this folder to Home** to get a new poster row.
- Select a movie to see its details and the list of video files. Subtitles (`.srt`, `.vtt`, `.ass`) found in
  the same folder are loaded automatically.
- Player: Left/Right = seek 10 s (controls hidden), OK = show controls / pause, Back = exit. Position is saved.
- **Settings** has the TMDB key, cache clearing and removing home rows.

## Limits to know about

- The server must be reachable from the TV (it may only work from inside a particular network).
- Some TVs cannot decode certain audio (DTS, AC3/E-AC3 on some models) or video (HEVC/x265 on older TVs).
  When playback fails the app offers to open the file in an external player such as VLC or MX Player.
- Debug builds are signed with a debug key, which is fine for installing on your own TV.
