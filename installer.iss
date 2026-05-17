[Setup]
AppName=IAE - Integrated Assignment Evaluator
AppVersion=1.0
AppPublisher=CE316 Project
DefaultDirName={localappdata}\IAE
DefaultGroupName=IAE
OutputDir=C:\projects\CE316Project\installer_output
OutputBaseFilename=IAE_Setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Files]
Source: "CE316Project\target\CE316Project-all.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "CE316Project\assignment_manager.db"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\IAE"; Filename: "{app}\IAE.bat"
Name: "{userdesktop}\IAE"; Filename: "{app}\IAE.bat"; Tasks: desktopicon

[Run]
Filename: "{app}\IAE.bat"; Description: "Launch IAE"; Flags: nowait postinstall skipifsilent

[Code]
procedure CreateBatchFile();
var
  BatchFile: string;
  Lines: TArrayOfString;
begin
  BatchFile := ExpandConstant('{app}\IAE.bat');
  SetArrayLength(Lines, 2);
  Lines[0] := '@echo off';
  Lines[1] := 'java -jar "' + ExpandConstant('{app}\CE316Project-all.jar') + '"';
  SaveStringsToFile(BatchFile, Lines, False);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    CreateBatchFile();
end;
