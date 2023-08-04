Global Enable3DMenu% = GetINIInt(OptionFile, "options", "enable 3d menu")
Global GameProgress = GetINIString$(OptionFile,"options","progress")
Global SelectedRoom$

Global Menu3D% = LoadImage_Strict("GFX\menu\menu3d.png")
ResizeImage(Menu3D, ImageWidth(Menu3D) * MenuScale, ImageHeight(Menu3D) * MenuScale)

Global menudark# = 1.0

Type menu3d
	Field obj%
	Field room$
	Field path$
	Field cam%
	Field menusprite[1]
	Field timer# = 70 * 5
	Field menupivots[4]
	Field menuasset[10]
End Type

;Global testmesh% ; for testing purposes

Function InitMenu3D.menu3d()
	Local m.menu3d = New menu3d
	
	DebugLog "INIT 3D MENU"
	
;	testmesh% = LoadAnimMesh_Strict("DevOnly\models\beta7.b3d")
;	ScaleEntity testmesh%, 0.054,0.054,0.054
;	PositionEntity testmesh%, 0,-2,5
;	EntityFX (testmesh%, 0.1)
;	;EntityOrder testmesh, -40
;	HideEntity testmesh
	
	m\cam = CreateCamera()
	CameraRange m\cam,0.1,40.0
	CameraProjMode m\cam,1
    CameraViewport m\cam,0,0,GraphicWidth,GraphicHeight
	HideEntity m\cam
	;HideEntity m\obj
	
	m\menusprite[0] = CreateSprite()
	ScaleSprite m\menusprite[0],2.0,2.0
	MoveEntity m\menusprite[0],0.0,0.0,1.0
	EntityOrder m\menusprite[0],-98
	EntityColor m\menusprite[0],0,0,0
	EntityParent m\menusprite[0],m\cam
	
	SeedRnd MilliSecs()
	
	Select GameProgress ; Select the game progress room
		Case "lcz" ; light containment zone
			SelectedRoom = "start"
		Case "hcz" ; heavy containment zone
			
		Case "ez" ; entrance zone
			
	End Select
	
	m\room = SelectedRoom
	m\path = GetINIString("Data\rooms.ini", SelectedRoom, "mesh path") ; get the mesh path in the ini file
	
	m\obj% = LoadRMesh_menu3d(m\path)
	ScaleEntity m\obj%, RoomScale, RoomScale, RoomScale
	HideEntity m\obj%
	
	Select m\room
		Case "room2"
			PositionEntity m\cam, -0.5, 0.5, -2.5
			TurnEntity(m\cam, 0, -40, 0)
			m\timer# = 70 * 5
	End Select
	
	ClearTextureCache
	
End Function

Function UpdateMenu3D()
	Local m.menu3d
	
	DebugLog "UPDATING 3D MENU"
	
	For m.menu3d = Each menu3d
		ShowEntity m\cam
		;ShowEntity testmesh
		ShowEntity m\obj
		CameraProjMode m\cam,1 ;Turns on the projection mode	
		CameraViewport m\cam,0,0,GraphicWidth,GraphicHeight ;This ports the projector
		RenderWorld ;Renders the world
		CameraProjMode m\cam,0 ;Disable the projector
		EntityAlpha m\menusprite[0],menudark
		m\timer# = m\timer# - FPSFactor
		
		
		Select m\room
			Case "start"
				AmbientLight Brightness, Brightness, Brightness
				CameraFogRange m\cam,0.1,6.0
				CameraFogMode m\cam,1
				PointEntity m\cam, m\obj
				PositionEntity m\cam, 13,2.4,4
				RotateEntity m\cam, 0,-40,0
		End Select
	Next
	
	If MainMenuTab=0 Then
		menudark=Max(menudark-FPSfactor*0.05,0.0)
	ElseIf MainMenuTab=1 Or MainMenuTab=2 Or MainMenuTab=3 Or MainMenuTab=5 Or MainMenuTab=6 Or MainMenuTab=7 Then
		menudark=Min(menudark+FPSfactor*0.05,0.7)
	Else
		menudark=Min(menudark+FPSfactor*0.05,0.7)
	EndIf
	
	;Animate2(testmesh%, AnimTime(testmesh), 1, 1537, 0.3)
	;TurnEntity testmesh%, 0, FPSfactor*0.45, 0
	
	UpdateWorld
	
End Function

Function NullMenu3D()
	Local m.menu3d
	SelectedRoom = ""
	
	;FreeEntity testmesh% : testmesh = 0
	
	For m.menu3d = Each menu3d
		FreeEntity m\cam : m\cam = 0
		m\room = 0
		m\path = 0
		FreeEntity m\obj : m\obj = 0
		m\menusprite[0] = 0
	Next
	
;	For i=0 To 4
;		FreeEntity m\menusprite[i] : m\menusprite[i] = 0
;	Next
	
	
	menudark = 1.0
	
	Delete Each menu3d
End Function

Function LoadRMesh_menu3d(file$) ;this ignores some stuff that we don't need
	
	;generate a texture made of white
	Local blankTexture%
	blankTexture=CreateTexture(4,4,1,1)
	ClsColor 255,255,255
	SetBuffer TextureBuffer(blankTexture)
	Cls
	SetBuffer BackBuffer()
	ClsColor 0,0,0
	
	;read the file
	Local f%=ReadFile(file)
	Local i%,j%,k%,x#,y#,z#,yaw#
	Local vertex%
	Local temp1i%,temp2i%,temp3i%
	Local temp1#,temp2#,temp3#
	Local temp1s$,temp2s$
	
	Local collisionMeshes% = CreatePivot()
	
	Local hasTriggerBox% = False ;See if it works on room that has triggerbox
	
	For i=0 To 3 ;reattempt up to 3 times
		If f=0 Then
			f=ReadFile(file)
		Else
			Exit
		EndIf
	Next
	If f=0 Then RuntimeError "Error reading file "+Chr(34)+file+Chr(34)
	Local isRMesh$ = ReadString(f)	
	If isRMesh$="RoomMesh"
		;Continue
	ElseIf isRMesh$="RoomMesh.HasTriggerBox"
		hasTriggerBox% = True
	Else
		RuntimeError Chr(34)+file+Chr(34)+" is Not RMESH ("+isRMesh+")"
	EndIf
	
	;If ReadString(f)<>"RoomMesh" Then RuntimeError Chr(34)+file+Chr(34)+" is not RMESH"
	
	DebugLog file
	file=StripFilename(file)
	
	Local count%,count2%
	;drawn meshes
	
	Local Opaque%,Alpha%
	
	Opaque=CreateMesh()
	Alpha=CreateMesh()
	
	count = ReadInt(f)
	Local childMesh%
	Local surf%,tex%[2],brush%
	
	Local isAlpha%
	
	Local u#,v#
	
	For i=1 To count ;drawn mesh
		childMesh=CreateMesh()
		
		surf=CreateSurface(childMesh)
		
		brush=CreateBrush()
		
		tex[0]=0 : tex[1]=0
		
		isAlpha=0
		For j=0 To 1
			temp1i=ReadByte(f)
			If temp1i<>0 Then
				temp1s=ReadString(f)
				tex[j]=GetTextureFromCache(temp1s)
				If tex[j]=0 Then ;texture is not in cache
					Select True
						Case temp1i<3
							tex[j]=LoadTexture(file+temp1s,1)
						Default
							tex[j]=LoadTexture(file+temp1s,3)
					End Select
					
					If tex[j]<>0 Then
						If temp1i=1 Then TextureBlend tex[j],5
						AddTextureToCache(tex[j])
						DebugLog StripPath(TextureName(tex[j]))
					EndIf
					
				EndIf
				If tex[j]<>0 Then
					isAlpha=2
					If temp1i=3 Then isAlpha=1
					
					TextureCoords tex[j],1-j
				EndIf
			EndIf
		Next
		
		If isAlpha=1 Then
			If tex[1]<>0 Then
				TextureBlend tex[1],2
				BrushTexture brush,tex[1],0,0
			Else
				BrushTexture brush,blankTexture,0,0
			EndIf
		Else
			
;			If BumpEnabled And temp1s<>"" Then
;				bumptex = GetBumpFromCache(temp1s)	
;			Else
;				bumptex = 0
;			EndIf
			
;			If bumptex<>0 Then 
;			BrushTexture brush, tex[1], 0, 0	
;			BrushTexture brush, bumptex, 0, 1
;			If tex[0]<>0 Then 
;				BrushTexture brush, tex[0], 0, 2	
;			Else
;				BrushTexture brush,blankTexture,0,2
;			EndIf
;			Else
			For j=0 To 1
				If tex[j]<>0 Then
					BrushTexture brush,tex[j],0,j
				Else
					BrushTexture brush,blankTexture,0,j
				EndIf
			Next				
;			EndIf
			
		EndIf
		
		surf=CreateSurface(childMesh)
		
		If isAlpha>0 Then PaintSurface surf,brush
		
		FreeBrush brush : brush = 0
		
		count2=ReadInt(f) ;vertices
		
		For j%=1 To count2
			;world coords
			x=ReadFloat(f) : y=ReadFloat(f) : z=ReadFloat(f)
			vertex=AddVertex(surf,x,y,z)
			
			;texture coords
			For k%=0 To 1
				u=ReadFloat(f) : v=ReadFloat(f)
				VertexTexCoords surf,vertex,u,v,0.0,k
			Next
			
			;colors
			temp1i=ReadByte(f)
			temp2i=ReadByte(f)
			temp3i=ReadByte(f)
			VertexColor surf,vertex,temp1i,temp2i,temp3i,1.0
		Next
		
		count2=ReadInt(f) ;polys
		For j%=1 To count2
			temp1i = ReadInt(f) : temp2i = ReadInt(f) : temp3i = ReadInt(f)
			AddTriangle(surf,temp1i,temp2i,temp3i)
		Next
		
		If isAlpha=1 Then
			AddMesh childMesh,Alpha
		Else
			AddMesh childMesh,Opaque
		EndIf
		EntityParent childMesh,Opaque
		EntityAlpha childMesh,0.0
		EntityType childMesh,HIT_MAP
		EntityPickMode childMesh,2
		
	Next
	
	If BumpEnabled Then; And 0 Then
;		For i = 1 To CountSurfaces(Opaque)
;			surf = GetSurface(Opaque,i)
;			brush = GetSurfaceBrush(surf)
;			tex[0] = GetBrushTexture(brush,1)
;			temp1s$ =  StripPath(TextureName(tex[0]))
;			DebugLog temp1s$
;			If temp1s$<>0 Then 
;				mat.Materials=GetCache(temp1s)
;				If mat<>Null Then
;					If mat\Bump<>0 Then
;						tex[1] = GetBrushTexture(brush,0)
;						
;						BrushTexture brush, tex[1], 0, 2	
;						BrushTexture brush, mat\Bump, 0, 1
;						BrushTexture brush, tex[0], 0, 0					
;						
;						PaintSurface surf,brush
;						
;						;If tex[1]<>0 Then DebugLog "lkmlkm" : FreeTexture tex[1] : tex[1]=0
;					EndIf
;				EndIf
;				
;				;If tex[0]<>0 Then DebugLog "sdfsf" : FreeTexture tex[0] : tex[0]=0
;			EndIf
;			If brush<>0 Then FreeBrush brush : brush=0
;		Next
;		
		For i = 2 To CountSurfaces(Opaque)
			sf = GetSurface(Opaque,i)
			b = GetSurfaceBrush( sf )
			If b<>0 Then
				t = GetBrushTexture(b, 1)
				If t<>0 Then
					texname$ =  StripPath(TextureName(t))
					
					For mat.Materials = Each Materials
						If texname = mat\name Then
							If mat\Bump <> 0 Then 
								t1 = GetBrushTexture(b,0)
								
								If t1<>0 Then
									BrushTexture b, t1, 0, 1 ;light map
									BrushTexture b, mat\Bump, 0, 0 ;bump
									BrushTexture b, t, 0, 2 ;diff
									
									PaintSurface sf,b
									
									FreeTexture t1
								EndIf
								
								;If t1<>0 Then FreeTexture t1
								;If t2 <> 0 Then FreeTexture t2						
							EndIf
							Exit
						EndIf 
					Next
					
					FreeTexture t
				EndIf
				FreeBrush b
			EndIf
			
		Next
	EndIf
	
;	Local hiddenMesh%
;	hiddenMesh=CreateMesh()
	
	count=ReadInt(f) ;invisible collision mesh
	For i%=1 To count
		;surf=CreateSurface(hiddenMesh)
		count2=ReadInt(f) ;vertices
		For j%=1 To count2
			;world coords
			x=ReadFloat(f) : y=ReadFloat(f) : z=ReadFloat(f)
			;vertex=AddVertex(surf,x,y,z)
		Next
		
		count2=ReadInt(f) ;polys
		For j%=1 To count2
			temp1i = ReadInt(f) : temp2i = ReadInt(f) : temp3i = ReadInt(f)
			;AddTriangle(surf,temp1i,temp2i,temp3i)
		Next
	Next
	
	;trigger boxes imported from window3d
	If hasTriggerBox
		numb = ReadInt(f)
		For tb = 0 To numb-1
			count = ReadInt(f)
			For i%=1 To count
				count2=ReadInt(f)
				For j%=1 To count2
					ReadFloat(f) : ReadFloat(f) : ReadFloat(f)
				Next
				count2=ReadInt(f)
				For j%=1 To count2
					ReadInt(f) : ReadInt(f) : ReadInt(f)
				Next
			Next
			ReadString(f)
		Next
	EndIf
	
;	count=ReadInt(f) ;point entities
;	For i%=1 To count
;		temp1s=ReadString(f)
;		Select temp1s
;			Case "model"
;				file = ReadString(f)
;				If file<>""
;					Local model = CreatePropObj("GFX\map\Props\"+file)
;					
;					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
;					PositionEntity model,temp1,temp2,temp3
;					
;					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
;					RotateEntity model,temp1,temp2,temp3
;					
;					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
;					ScaleEntity model,temp1,temp2,temp3
;					
;					EntityParent model,Opaque
;					EntityType model,HIT_MAP
;					EntityPickMode model,2
;				Else
;					DebugLog "file = 0"
;					temp1=ReadFloat(f) : temp2=ReadFloat(f) : temp3=ReadFloat(f)
;					DebugLog temp1+", "+temp2+", "+temp3
;				EndIf
;		End Select
;	Next
	
	Local obj%
	
	temp1i=CopyMesh(Alpha)
	FlipMesh temp1i
	AddMesh temp1i,Alpha
	FreeEntity temp1i
	
	If brush <> 0 Then FreeBrush brush
	
	AddMesh Alpha,Opaque
	FreeEntity Alpha
	
	EntityFX Opaque,3
	
	;EntityAlpha hiddenMesh,0.0
	EntityAlpha Opaque,1.0
	
	;EntityType Opaque,HIT_MAP
	;EntityType hiddenMesh,HIT_MAP
	FreeTexture blankTexture
	
;	obj=CreatePivot()
;	CreatePivot(obj) ;skip "meshes" object
;	EntityParent Opaque,obj
;	EntityParent hiddenMesh,obj
;	CreatePivot(Room) ;skip "pointentites" object
;	CreatePivot(Room) ;skip "solidentites" object
	
	CloseFile f
	
	Return Opaque
	
End Function
;~IDEal Editor Parameters:
;~F#90
;~C#Blitz3D