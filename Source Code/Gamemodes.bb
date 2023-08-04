Type gamemodes
	Field name$
	Field description$
	Field difficulty$
	Field img$
	Field selected%
	Field locked% = True
End Type

Dim gamemodes.gamemodes(2) ; table amount + 1
Global Selectedgamemodes.gamemodes
Global gamemodesImages[2]

;gamemodes (unfinished)
Global gamemode1% = GetINIInt(OptionFile, "gamemodes", "mode 1 locked")

;PutINIValue(OptionFile, "gamemodes", "mode 1 locked", "true")

;[Block]
gamemodes(0) = New gamemodes
gamemodes(0)\name = "突破"
gamemodes(0)\description = "在对你的测试开始后不久，你目前居住的设施就发生了大规模的安全漏洞，让你可以与所有在该设施周围漫游的逃跑实体一起逃跑。你的目标是走出设施，在世界各地拥抱."
gamemodes(0)\img = "cb"
gamemodes(0)\selected% = False
gamemodes(0)\locked% = False

gamemodes(1) = New gamemodes
gamemodes(1)\selected% = False
gamemodes(1)\locked% = gamemode1%
;[End Block]

Selectedgamemodes = gamemodes(0);gamemodes(GetINIInt(OptionFile, "gamemodes", "selectedmode"))

For i = 0 To 1
	If gamemodes(i)\img <> "" Then
		gamemodesImages[i] = LoadImage_Strict("GFX\menu\gamemodes\"+gamemodes(i)\img+".png")
		ResizeImage(gamemodesImages[i], 90*MenuScale, 90*MenuScale)
		MaskImage(gamemodesImages[i], 255, 0, 255)
	EndIf
Next

;~IDEal Editor Parameters:
;~C#Blitz3D