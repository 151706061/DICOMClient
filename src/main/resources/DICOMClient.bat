@echo off

@rem Get the directory that this script resides in
for %%F in (%0) do set dirname=%%~dpF

@rem set JAVA_HOME=\\robkup\TPSData\java\jre6-32
set JAVA_HOME=S:\Physics\Projects\jre6-32

set PATH=%JAVA_HOME%\bin;%PATH%

@rem set the current directory so that logs will go in the right place
cd %dirname%

java -Xmx256m -cp %dirname%@@with-dep-jar@@ -Djava.util.logging.config.file=%dirname%logging.propertiesWindows edu.umro.dicom.client.DicomClient %*
