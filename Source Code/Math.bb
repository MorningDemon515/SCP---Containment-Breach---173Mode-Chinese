Function GenerateSeedNumber(seed$)
 	Local temp% = 0
 	Local shift% = 0
 	For i = 1 To Len(seed)
 		temp = temp Xor (Asc(Mid(seed,i,1)) Shl shift)
 		shift=(shift+1) Mod 24
	Next
 	Return temp
End Function

Function CurveValue#(number#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
	If number < old Then
		Return Max(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	Else
		Return Min(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	EndIf
End Function

Function CurveAngle#(val#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
	Local diff# = WrapAngle(val) - WrapAngle(old)
	If diff > 180 Then diff = diff - 360
	If diff < - 180 Then diff = diff + 360
	Return WrapAngle(old + diff * (1.0 / smooth * FPSfactor))
End Function

; This is necessary because negative numbers modulod yield negative numbers, thanks for your help Juan! :)
Function WrapAngle#(angle#)
	If angle = Infinity Then Return 0.0
	If angle < 0 Then
		Return 360 + (angle Mod 360)
	Else
		Return angle Mod 360
	EndIf
End Function

Function GetAngle#(x1#, y1#, x2#, y2#)
	Return ATan2( y2 - y1, x2 - x1 )
End Function

Function point_direction#(x1#,z1#,x2#,z2#)
	Local dx#, dz#
	dx = x1 - x2
	dz = z1 - z2
	Return ATan2(dz,dx)
End Function

Function point_distance#(x1#,z1#,x2#,z2#)
	Local dx#,dy#
	dx = x1 - x2
	dy = z1 - z2
	Return Sqr((dx*dx)+(dy*dy)) 
End Function

Function angleDist#(a0#,a1#)
	Local b# = a0-a1
	Local bb#
	If b<-180.0 Then
		bb = b+360.0
	Else If b>180.0 Then
		bb = b-360.0
	Else
		bb = b
	EndIf
	Return bb
End Function

Function Inverse#(number#)
	
	Return Float(1.0-number#)
	
End Function

Function Rnd_Array#(numb1#,numb2#,Array1#,Array2#)
	Local whatarray% = Rand(1,2)
	
	If whatarray% = 1
		Return Rnd(Array1#,numb1#)
	Else
		Return Rnd(numb2#,Array2#)
	EndIf
	
End Function
;~IDEal Editor Parameters:
;~C#Blitz3D