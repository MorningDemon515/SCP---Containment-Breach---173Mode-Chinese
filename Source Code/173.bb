; One of the main files of the mod (required for the mod to function)
; This bb file contains most of the variables and functions used in the mod
Const ModVersion$ = "0.3.1"
Global BodyModel%, HeadModel%, CamPivot%, rotation%, ContainmentBox%
Global Looking% = False
Global moving% = False
Global scp173j% = False
Global corrosive% = False
Global breaktestroom2glass% = False
Global playclosetevent% = False
Global ClosestVentOpening%, UpdateVentsTimer#
Dim Scp173SFX%(3)
Global followpvt = CreatePivot()
Global containtimer = 70 * 15 ;Rand(35,45)
Global imageindex%

Include "Gamemodes.bb"

Global GAMEMODE% = 1

Type Vents
	Field x%,y%,z%
	Field scalex%,scalez%
	Field obj%
	Field interactsprite%
End Type

Type BreakableObjects
	Field x%,y%,z%
	Field x2%,y2%,z2%
	Field obj%
	Field tex%
	Field objtype$
	Field room.Rooms
	Field interactsprite%
	Field broken% = False
End Type

Type Cutscenes
	Field timer#, eventtype%, isplaying% = False
End Type

Global VentMenuOpen% = False

Global Contained% = False
Global EnableHeadTurning = GetINIInt(OptionFile, "options", "enable head turning")

Global freecam% = False

Function Update173()
	;[Block]
	Curr173\Idle = 3 ;Disable NPC 173
	HideEntity Curr173\obj
	HideEntity Curr173\Collider
	
	Curr106\Idle = True ;Disable SCP-106
	Curr106\State = 200000
	Contained106 = True
	
	NoTarget% = True
	
	If Contained% Then
		ShowEntity ContainmentBox
		Looking = True
		PositionEntity(BodyModel, EntityX(Collider), EntityY(Collider) + 0.05 + Sin(MilliSecs()*0.08)*0.02, EntityZ(Collider))
		RotateEntity (ContainmentBox, 0, EntityYaw(BodyModel)-180, 0)
		
		containtimer = Max(containtimer-FPSfactor, 0.0)
	Else		HideEntity ContainmentBox
	EndIf
	
	If corrosive Then
		Local texture = LoadTexture_Strict("GFX\npcs\173-106.png", 1)
		EntityTexture BodyModel, texture, 0, 0
		EntityTexture HeadModel, texture, 0, 0
		FreeTexture texture
	EndIf
	
	PositionEntity followpvt, EntityX(Collider), EntityY(Collider), EntityZ(Collider)
	
	;[End Block]
End Function

Function StatueSFX(model,ori)
	;[Block]
	Local dist#
	dist# = EntityDistance(model, ori)		
	
	If dist < 1.5 And Rand(700) = 1 Then PlaySound2(Scp173SFX(Rand(0, 2)), Camera, BodyModel)
	
	;[End Block]
End Function

Function ReturnChangeValue(blinktimer#)
	Local change = blinktimer / 4 /70
	DebugLog "CHANGE: "+change
	DebugLog "nblinktimer: "+blinktimer
	
	Return change
End Function

Function VentSelectionFrame(x%, y%, width%, height%, location$)
	Color (255, 0, 0)
	DrawTiledImageRect(MenuBlack, (x Mod 256), (y Mod 256), 512, 512, x, y, width, height)
	DrawTiledImageRect(MenuWhite, (x Mod 256), (y Mod 256), 512, 512, x + 4, y + 4, width - 8, height - 8)
	Local selected%
	Local Highlight% = MouseOn(x, y, width, height)
	
	If Highlight Then
		Color(50, 50, 50)
		If MouseHit1 Then 
			selected = (Not selected)
			GotoVentLocation(location$)
		EndIf
	Else
		Color(0, 0, 0)		
	EndIf
End Function

Function DrawRoomFrame(x%, y%, width%, height%, xoffset%=0, yoffset%=0)
	Color 100, 100, 100
	
	DrawTiledImageRect(MenuWhite, yoffset, (y Mod 256), 512, 512, x+4*MenuScale, y+4*MenuScale, width*MenuScale, height*MenuScale)	
End Function

Function UpdateNPCBlink(np.NPCs)
	If np\NPCType = NPCtypeMTF2
		If np\BlinkTimer < 70*1 And np\BlinkTimer > 70*0.98571428571 And Contained% <> True Then ; if the npc's blinktimer is exactly 70
			np\SoundChn2 = PlaySound_Strict (LoadTempSound("SFX\Character\MTF\173\newlines\173blinking"+Rand(1,7)+".ogg"))
		EndIf
		
		If np\MTFLeader = Null Then
			np\State3=np\State3+FPSfactor
			
			If np\State3 <= 70.0*11.0 And np\State3 >= 70.0*10.98571428571 Then
				np\SoundChn2 = PlaySound_Strict (LoadTempSound("SFX\Character\MTF\173\newlines\173cage_prep"+Rand(1,2)+".ogg"))
				DebugLog "Deploying Cage E-9"
			EndIf
			
			If np\State3 <= 70.0*23.0 And np\State3 >= 70.0*22.98571428571 Then
				If np\SoundChn2 <> 0 Then StopChannel(np\SoundChn2)
				np\SoundChn2 = PlaySound_Strict (LoadTempSound("SFX\Character\MTF\173\newlines\173cage_depl"+Rand(1,2)+".ogg"))
				DebugLog "We have deployed Cage E-9"
				ShowEntity ContainmentBox
			EndIf
			
			If np\State3>=70.0*25.0 Then
				If DebugHUD Then
					EntityColor np\obj, 0,255,0
				Else
					EntityColor np\obj, 255,255,255
				EndIf
				If (Not Contained%) Then 
					PlaySound_Strict (LoadTempSound("SFX\Character\MTF\173\CageActivate.ogg"))
				EndIf
				
				Contained% = True
				
				np\State = 1
			EndIf
		EndIf
	EndIf 
End Function

Function RenderSmoothBar(x%, y%, Width%, Height%, Value1#, Value2# = 100.0)
	Local i%
	Color(100, 100, 100)
	Rect(x + (3 * MenuScale), y + (3 * MenuScale), Float((Width - (2 * MenuScale)) * (Value1 / Value2)), 14 * MenuScale)
End Function

Function CreateVentSpot.Vents(x#, y#, z#, r.Rooms, xscale#, zscale#)
	Local v.Vents = New Vents
	DebugLog "Created Vent access point at: "+x+", "+y+", "+z
	
	v\interactsprite = CreateSprite()
	Local tex% = LoadTexture_Strict("GFX\interactvent.png",1+2)
	ScaleSprite v\interactsprite,0.05,0.05
	EntityOrder v\interactsprite,-1
	EntityTexture v\interactsprite,tex
	EntityFX v\interactsprite, 1+8
	
	v\obj% = CreatePivot();CopyEntity(VentAccessOBJ)
	;EntityAlpha v\obj, 0.5
	;ScaleEntity(v\obj, xscale, RoomScale, zscale)
	PositionEntity(v\obj, x, y, z)
	;HideEntity v\obj
	
	If r<>Null Then EntityParent(v\obj, r\obj)
	
	;PositionEntity v\interactsprite, EntityX(v\obj, True), EntityY(v\obj, True), EntityZ(v\obj, True)
	
	Return v
End Function

Function CreateBreakableObject.BreakableObjects(x#, y#, z#, scalex#, scaley#, scalez#, x2#, y2#, z2#, objecttype$="glass", r.Rooms)
	Local b.BreakableObjects = New BreakableObjects
	
	b\room = r
	
	If objecttype$ = "glass" Then
		
		b\interactsprite = CreateSprite()
		Local tex% = LoadTexture_Strict("GFX\interactwindow.png",1+2)
		ScaleSprite b\interactsprite,0.05,0.05
		EntityOrder b\interactsprite,-1
		EntityTexture b\interactsprite,tex
		EntityFX b\interactsprite, 1+8
		
		b\objtype$ = "glass_room2testroom2"
		Local Glasstex = LoadTexture_Strict("GFX\map\glass.png",1+2)
		b\obj = CreateSprite()
		EntityTexture(b\obj,Glasstex)
		SpriteViewMode(b\obj,2)
		ScaleSprite(b\obj, 182.0*RoomScale*0.5, 192.0*RoomScale*0.5)
		PositionEntity(b\obj, x, y, z)
		RotateEntity b\obj, 0,180,0
		TurnEntity(b\obj,0,180,0)			
		EntityParent(b\obj, r\obj)
		FreeTexture Glasstex
	EndIf
End Function

Function UpdateVents()
	Local v.Vents
	
	For v.Vents = Each Vents
		If EntityDistance(Collider, v\obj) < 2 Then
			If EntityInView(v\obj, Camera) Then 
				If ClosestVentOpening = 0 Then
					ClosestVentOpening = v\obj
					
					PositionEntity v\interactsprite, EntityX(ClosestVentOpening, True), EntityY(ClosestVentOpening, True), EntityZ(ClosestVentOpening, True)
					ShowEntity(v\interactsprite)
					EntityColor v\interactsprite, 255-(Float(Sin(MilliSecs()/4.0))*150),255-(Float(Sin(MilliSecs()/4.0))*150),0
					
					If MouseHit1 Then;If MouseHit1 Then
						VentMenuOpen% = True
					ElseIf MouseHit2 Then
						VentMenuOpen% = False
					EndIf
				EndIf
			Else
				HideEntity(v\interactsprite)
				VentMenuOpen% = False
			EndIf
		Else
			HideEntity(v\interactsprite)
			ClosestVentOpening = 0
		EndIf
	Next
End Function

Function UpdateBreakableObjects()
	Local b.BreakableObjects
	
	For b.BreakableObjects = Each BreakableObjects
		Select b\objtype$
			Case "glass_room2testroom2"
				If breaktestroom2glass% = True Then
					RotateEntity b\obj, 0,0,0
					If (Not b\broken) Then
						If EntityDistance(Collider, b\obj) < 1 And EntityHidden(b\obj) <> True Then
							If EntityInView(b\obj, Camera) Then 
								
								PositionEntity b\interactsprite, EntityX(b\obj, True), EntityY(b\obj, True), EntityZ(b\obj, True)
								ShowEntity(b\interactsprite)
								EntityColor b\interactsprite, 255-(Float(Sin(MilliSecs()/4.0))*150),255-(Float(Sin(MilliSecs()/4.0))*150),0
								
								;EntityFX b\obj,1+8+32 : EntityColor b\obj,255-(Float(Sin(MilliSecs()/4.0))*150),255-(Float(Sin(MilliSecs()/4.0))*150),0
								If MouseHit1 Then
									b\broken = True
									CameraShake = 3.0
									PlaySound2(LoadTempSound("SFX\General\GlassBreak.ogg"), Camera, b\obj)
									HideEntity b\obj
									PositionEntity(Collider, EntityX(b\room\Objects[1], True), 0.5, EntityZ(b\room\Objects[1], True))
									ResetEntity(Collider)
									HideEntity(b\interactsprite)
								EndIf
							Else
								HideEntity(b\interactsprite)
							EndIf
						EndIf
					Else
						HideEntity b\obj
						HideEntity b\interactsprite
						b\broken = True
					EndIf
				Else
					RotateEntity b\obj, 0,180,0
				EndIf
		End Select
	Next
End Function

Function IsVentRoom(room$)
	If room$ = "room2closets" Or room$ = "room2doors" Or room$ = "lockroom" Or room$ = "tunnel2" Then ;Or room$ = "room2testroom2" Then
		Return True
	Else
		Return False
	EndIf
End Function

Function GotoVentLocation(location$)
	Local v.Vents, r.Rooms
	Local pvt%
	
	For r.Rooms = Each Rooms ;thank blitz3D for this
		If r\RoomTemplate\Name = "lockroom" And location$ = "lockroom" Or r\RoomTemplate\Name = "start" And location$ = "start" Or r\RoomTemplate\Name = "room2doors" And location$ = "room2doors" Or r\RoomTemplate\Name = "room2testroom2" And location$ = "room2testroom2" Or r\RoomTemplate\Name = "room2closets" And location$ = "room2closets" Or r\RoomTemplate\Name = "tunnel2" And location$ = "tunnel2" Then
			pvt% = CreatePivot()
			PositionEntity pvt%, EntityX(r\RoomVents[1]\obj%, True), EntityY(r\RoomVents[1]\obj%, True), EntityZ(r\RoomVents[1]\obj%, True)
			EntityParent pvt%, r\obj
			BlinkTimer = -10.0 : LightBlink = 5.0
			PlayerRoom = r
			TeleportEntity(Collider%, EntityX(pvt%,True), EntityY(pvt%,True), EntityZ(pvt%,True), 0, True) : PlaySound_Strict(LoadSound_Strict("SFX\SCP\173\173Vent.ogg"))
			UpdateDoorsTimer = 0.0
			UpdateDoors()
			UpdateRooms()
			VentMenuOpen% = False
			FreeEntity pvt%
		EndIf
	Next
	
	Looking% = False
End Function

Function RelightRoom(r.Rooms,tex%,oldtex%) ;replace a lightmap texture
	Local mesh=GetChild(r\obj,2)
	Local surf%,brush%,tex2%,texname$,temp%,temp2%
	Local comparison$ = StripPath(TextureName(oldtex))
	temp=(BumpEnabled+1)
	For i=1 To CountSurfaces(mesh)
		temp2=temp
		surf=GetSurface(mesh,i)
		brush=GetSurfaceBrush(surf)
		If brush<>0 Then
			tex2=GetBrushTexture(brush,temp2)
			If tex2=0 And temp2>1 Then tex2=GetBrushTexture(brush,0) : temp2=1
			If tex2<>0 Then
				texname=TextureName(tex2)
				If StripPath(texname)=comparison Then
					BrushTexture brush,tex,0,temp
					PaintSurface surf,brush
				EndIf
				FreeTexture tex2
			EndIf
			FreeBrush brush
		EndIf
	Next
End Function

Function FPSMainMenu(fpsamount%)
	AASetFont ConsoleFont	
	If ShowFPS Then AAText 20, GraphicHeight-70, "FPS: " + fpsamount
End Function

Function IsNPCStuck(n.NPCs,time#)
	Local x#,y#,z#
	Local timer#
	
	timer=Max(timer-FPSfactor,0.0)
	;DebugLog("Stuck Timer: "+timer)
	If timer<=0.0 Then
		If EntityX(n\Collider)=x Then
			If EntityZ(n\Collider)=z Then
				Return True
			EndIf
		EndIf
		timer=time
	EndIf
	If timer=time/2 Then
		x = EntityX(n\Collider)
		y = EntityY(n\Collider)
		z = EntityZ(n\Collider)
	EndIf
	
	Return False
End Function
;~IDEal Editor Parameters:
;~F#54#5E#66#78#7E#A5#AB#C3#DF#FD#127#12F#146#15F#164
;~C#Blitz3D