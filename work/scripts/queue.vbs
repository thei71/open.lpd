' get queue and print job folder

queueName = WScript.Arguments(0)
printJobFolder = WScript.Arguments(1)

' find data file and print

Set Fso = WScript.CreateObject("Scripting.FileSystemObject")
Set objFolder = Fso.GetFolder(printJobFolder)
Set colFiles = objFolder.Files
For Each objFile in colFiles
	If InStr(objFile.Name, "dfA") = 1 Then
 		dataFileName = printJobFolder & "\" & objFile.Name
 		If InStr(dataFileName, "." & queueName) = Len(dataFileName) - Len(queueName) Then
			printFileName = dataFileName
		Else
			printFileName = dataFileName & "." & queueName
			Fso.MoveFile dataFileName, printFileName
		End If
		Set objShell = CreateObject("Shell.Application")
		objShell.ShellExecute printFileName, "", "", "open", 1
	End If
Next
