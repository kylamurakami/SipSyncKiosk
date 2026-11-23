SipSync Cafe Kiosk (Java Swing)

HOW TO RUN
 - Command line: run run.bat (Windows) or run.sh, from this folder.
 - IDE (NetBeans/IntelliJ/Eclipse): open this folder as a project, set src as the source folder,
   run Main.java. The working directory must be the project folder (the one containing "images").

LOGINS (hard-coded, one per role)
 Staff / Barista : staff / staff123
 Administrator   : admin / admin123

NOTES
 - Orders live in memory (DataStore). Customer orders appear on the Staff screen while the app is running.
 - Quiz results that are not on the menu map to the closest menu item (edit the table in PersonalityQuiz.java).
