<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:prop="http://saxonica.com/ns/html-property"
xmlns:style="http://saxonica.com/ns/html-style-property"
xmlns:js="http://saxonica.com/ns/globalJS" xmlns:xs="http://www.w3.org/2001/XMLSchema"
version="2.0" xmlns:f="http://www.saxonica.com/chessGame" exclude-result-prefixes="xs prop f"
extension-element-prefixes="ixsl prop style">

    
<xsl:variable name="straightStep" select="(0,1), (1,0), (0,-1), (-1,0)"/>
<xsl:variable name="diagonalStep" select="(1,1), (1,-1), (-1,-1), (-1,1)"/>
<xsl:variable name="kingPositions" select="(3, 7, 59, 63)"/>
<xsl:variable name="oldRookPositions" select="(1, 8, 57, 64)"/>
<xsl:variable name="castlePositions" select="(4, 6, 60, 62)"/>
 
<xsl:function name="f:isValidStraightMove" as="element(move-test)">
<xsl:param name="piece" as="element(div)"/>
<xsl:param name="from" as="xs:integer"/>
<xsl:param name="to" as="xs:integer"/>
<xsl:param name="board" as="element(div)*"/>
<xsl:param name="unitSteps" as="xs:integer*"/>
<xsl:param name="maxDistance" as="xs:integer"/>
<xsl:variable name="destinationAvailable"
select="$board[$to]/div/@data-colour != $piece/@data-colour" />
<xsl:variable name="rowDistance" as="xs:integer"
select="f:row($to) - f:row($from)"/>
<xsl:variable name="columnDistance" as="xs:integer"
select="f:column($to) - f:column($from)"/>
<xsl:variable name="rowDirection" as="xs:integer"
select="f:signum($rowDistance)"/>
<xsl:variable name="columnDirection" as="xs:integer"
select="f:signum($columnDistance)"/>

<xsl:variable name="isKing" as="xs:boolean"
select="$piece/@data-piece eq 'king'"/>
<xsl:variable name="isWhite" as="xs:boolean"
select="$piece/@data-colour eq 'white'"/>

<xsl:variable name="castleIndex" as="xs:integer?" 
select="index-of($kingPositions, $to)"/>
<xsl:variable name="castlePosition" as="xs:integer" 
select="if (exists($castleIndex)) then
$castlePositions[$castleIndex]
else 0"/>
<xsl:variable name="oldRookPosition" as="xs:integer"
select="if (exists($castleIndex)) then
$oldRookPositions[$castleIndex]
else 0"/>
<xsl:variable name="knightCastleEmpty" as="xs:boolean"
select="if ($oldRookPosition = (1, 57)) then
$board[$oldRookPosition + 1]/div/@data-piece eq 'empty'
else
true()"/>
<xsl:variable name="requiredRookColour" as="xs:string"
select="if (exists($castleIndex)) then
    $board[$oldRookPosition]
    /div[@data-piece eq 'rook']/@data-colour
else '' "/>
<xsl:variable name="rookOkForCastling" as="xs:boolean"
select="exists($requiredRookColour)
and (
($isWhite and $requiredRookColour eq 'white')
     or 
 (not($isWhite) and $requiredRookColour eq 'black')
 )
"/>
<xsl:variable name="isCastling" as="xs:boolean"
select="$isKing
and (
    ($isWhite and $from eq 61) 
    or (not($isWhite) and $from eq 5)
)
and $rowDistance eq 0
and abs($columnDistance) eq 2
and $board[$castlePosition]/div/@data-piece eq 'empty'
and $knightCastleEmpty
"/>


<xsl:variable name="direction" as="xs:integer?"
select="for $i in 1 to count($unitSteps) idiv 2 return
if (
$unitSteps[$i * 2 - 1] = $rowDirection and
$unitSteps[$i * 2] = $columnDirection)
then $i
else ()"/>

<xsl:variable name="isSingleStep" as="xs:boolean"
select="$rowDistance eq $rowDirection
and $columnDistance eq $columnDirection"/>
<xsl:variable name="pathUnblocked" as="xs:boolean"
select="$isSingleStep or
($maxDistance gt 1 and 
f:pathIsUnblocked(
f:row($from), f:column($from),
f:row($to), f:column($to),
$board, $rowDirection, $columnDirection
))"/>
<xsl:message>
<xsl:value-of select="'&#10;',
'piece', $piece/@data-piece, '&#10;',
'from/to', concat(string($from), '-', string($to)), '&#10;',
'unitSteps', string-join(for $u in $unitSteps return string($u), ','), '&#10;',
'maxDistance', string($maxDistance), '&#10;',
'destAvailable:', $destinationAvailable, '&#10;',
'rowDistance:', $rowDistance, '&#10;',
'reqRookColour:', $requiredRookColour, '&#10;',
'isCastling:', $isCastling, '&#10;',
'columnDistance:', $columnDistance, '&#10;',
'rowDirection:', $rowDirection, '&#10;',
'columndirection:', $columnDirection, '&#10;',
'singleStep:', $isSingleStep, '&#10;',
'pathUnblocked:', $pathUnblocked, '&#10;',
'direction:', string-join((for $d in $direction return string($d)), ','),  '&#10;'"/>
</xsl:message>

<xsl:variable name="is-valid" as="xs:boolean"
select="$isCastling or
($destinationAvailable and $pathUnblocked and exists($direction))"/>

<move-test is-valid="{if ($is-valid) then 'yes' else 'no'}">
<xsl:if test="$isCastling">
<xsl:attribute name="old-rook" select="$oldRookPosition"/>
<xsl:attribute name="new-rook" select="$castlePosition"/>
<xsl:attribute name="castling-colour" select="$requiredRookColour"/>
</xsl:if>
</move-test>

</xsl:function>

<xsl:function name="f:pathIsUnblocked" as="xs:boolean">
<xsl:param name="fromRow" as="xs:integer"/>
<xsl:param name="fromColumn" as="xs:integer"/>
<xsl:param name="toRow" as="xs:integer"/>
<xsl:param name="toColumn" as="xs:integer"/>
<xsl:param name="board" as="element(div)*"/>
<xsl:param name="rowDirection" as="xs:integer"/>
<xsl:param name="columnDirection" as="xs:integer"/>

<xsl:variable name="nextRow" as="xs:integer"
select="$fromRow + $rowDirection"/>
<xsl:variable name="nextColumn" as="xs:integer"
select="$fromColumn + $columnDirection"/>
<xsl:variable name="square" select="$board[f:square($nextRow, $nextColumn)]"/>

<xsl:message>
<xsl:value-of select="'isunblocked?',
'square:', string($square/@id),
'piece:', string($square/div/@data-piece)"/>
</xsl:message>

<xsl:sequence select="($nextRow eq $toRow and
$nextColumn eq $toColumn)
or
(
$square/div/@data-piece eq 'empty'
and
f:pathIsUnblocked(
$nextRow, $nextColumn,
$toRow, $toColumn,
$board, $rowDirection, $columnDirection
))"/>
</xsl:function>

<xsl:function name="f:signum" as="xs:integer">
<xsl:param name="in" as="xs:integer"/>
<xsl:sequence select="if ($in eq 0) then
0 else $in idiv abs($in)"/>
</xsl:function>      

<xsl:template match="div[@data-piece='bishop']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element()*"/>
<xsl:sequence select="f:isValidStraightMove(., $moveFrom, $moveTo, $board, $diagonalStep, 7)"/>
</xsl:template>

<xsl:template match="div[@data-piece='rook']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element()*"/>
<xsl:sequence select="f:isValidStraightMove(., $moveFrom, $moveTo, $board, $straightStep, 7)"/>
</xsl:template>

<xsl:template match="div[@data-piece='queen']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element()*"/>
<xsl:sequence select="f:isValidStraightMove(., $moveFrom, $moveTo, $board, ($straightStep, $diagonalStep), 7)"/>
</xsl:template>

<xsl:template match="div[@data-piece='king']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element()*"/>
<xsl:sequence select="f:isValidStraightMove(., $moveFrom, $moveTo, $board, ($straightStep, $diagonalStep), 1)"/>
</xsl:template>

<xsl:template match="div[@data-piece='knight']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element(div)+"/>
<xsl:variable name="destinationAvailable"
select="not($board[$moveTo]/@data-colour = @data-colour)" />
<xsl:variable name="rowDistance" as="xs:integer"
select="f:row($moveTo) - f:row($moveFrom)"/>
<xsl:variable name="columnDistance" as="xs:integer"
select="f:column($moveTo) - f:column($moveFrom)"/>
<xsl:variable name="is-valid" as="xs:boolean"
select="$destinationAvailable and abs($rowDistance) *
abs($columnDistance) = 2"/>

<move-test is-valid="{if ($is-valid) then 'yes' else 'no'}"/>
</xsl:template>

<xsl:template match="div[@data-piece='pawn']" mode="is-valid-move" as="element(move-test)">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element(div)+"/>
<xsl:variable name="fromRow" select="f:row($moveFrom)"/>
<xsl:variable name="toRow" select="f:row($moveTo)"/>
<xsl:variable name="fromCol" select="f:column($moveFrom)"/>
<xsl:variable name="toCol" select="f:column($moveTo)"/>
<xsl:variable name="maxDistance" select="2" as="xs:integer"/>
<xsl:variable name="toColour" select="$board[$moveTo]/div/@data-colour" as="xs:string"/>
<xsl:variable name="destinationAvailable" as="xs:boolean"
select="$toColour eq 'undefined'" />
<xsl:variable name="isWhite" as="xs:boolean"
select="@data-colour eq 'white'"/>
<xsl:variable name="opposedColour" as="xs:string"
select="if ($isWhite)
then 'black' else 'white'"/>
<xsl:variable name="destinationOpposed"
select="$toColour eq $opposedColour"/>
<xsl:variable name="enpassantPos" as="xs:integer"
select="if ($isWhite) then
$moveTo + 8
else
$moveTo - 8" />
<!-- if a pawn exists one row ahead of the 'to' position -->
<xsl:variable name="enPassantPawn" as="xs:boolean"
select="exists($board[$enpassantPos]/div[@data-colour eq $opposedColour and @data-piece eq 'pawn'])"/>
<!-- if the 'to' row makes it possible for en passant -->
<xsl:variable name="isEnPassant" as="xs:boolean"
select="($isWhite and $toRow eq 6)
or (not($isWhite) and $toRow eq 3)
and $destinationAvailable
and $enPassantPawn"/>

<xsl:variable name="pre-rowDistance" as="xs:integer"
select="f:row($moveTo) - f:row($moveFrom)"/>
<!-- to ensure valid rowDistance is positive for black or white: -->
<xsl:variable name="rowDistance" select="if (@data-colour eq 'black') then
$pre-rowDistance * -1
else $pre-rowDistance"/>
<xsl:variable name="unmoved" as="xs:boolean"
select="if ($isWhite) then
$moveFrom gt (6 * 8)
else
$moveFrom le 16"/>
<xsl:variable name="columnDistance" as="xs:integer"
select="f:column($moveTo) - f:column($moveFrom)"/>
<xsl:variable name="rowDirection" as="xs:integer"
select="f:signum($rowDistance)"/>
<xsl:variable name="columnDirection" as="xs:integer"
select="f:signum($columnDistance)"/>

<xsl:variable name="columnOK" as="xs:boolean"
select="if ($destinationOpposed or $isEnPassant) then
abs($columnDistance) eq 1
else $columnDistance eq 0"/>

<xsl:variable name="rowOK" as="xs:boolean"
select="if ($destinationOpposed or $isEnPassant or not($unmoved)) then
$rowDistance eq 1
else
$rowDistance = (1,2)"/>

<xsl:variable name="isSingleStep" as="xs:boolean"
select="$rowDistance lt 2"/>
<xsl:variable name="nextRowPos" as="xs:integer"
select="if ($isWhite) then
$moveFrom + 8
else
$moveFrom - 8
" />
<xsl:variable name="blockingPiece" as="xs:boolean"
select="if ($isSingleStep) then
false() else
$board[$nextRowPos]/div/@data-piece eq 'empty'
"/>

<xsl:message>
<xsl:value-of select="'&#10;',
'piece', @data-piece, '&#10;',
'from/to', concat(string($moveFrom), '-', string($moveTo)), '&#10;',
(:'unitSteps', string-join(for $u in $unitSteps return string($u), ','), '&#10;',:)
'maxDistance', string($maxDistance), '&#10;',
'destAvailable:', $destinationAvailable, '&#10;',
'rowDistance:', $rowDistance, '&#10;',
'columnDistance:', $columnDistance, '&#10;',
'rowDirection:', $rowDirection, '&#10;',
'columndirection:', $columnDirection, '&#10;',
'unmoved:', string($unmoved), '&#10;',
'singleStep:', $isSingleStep, '&#10;',
'rowOK:', $rowOK, '&#10;',
'columnOK:', $columnOK, '&#10;',
'enPassantPawn:', string($enPassantPawn), '&#10;',
'enPassant:', string($isEnPassant), '&#10;',
'destOpposed:', $destinationOpposed, '&#10;'"/>

</xsl:message>
<xsl:variable name="is-valid" as="xs:boolean"
select="$rowOK and $columnOK and not($blockingPiece)"/>

<move-test is-valid="{if ($is-valid) then 'yes' else 'no'}">
<xsl:if test="$isEnPassant">
<xsl:attribute name="enpassant-pos" select="$enpassantPos"/>
</xsl:if>
</move-test>

</xsl:template>

<xsl:function name="f:isValidMove" as="element(piece-move)">
<xsl:param name="piece" as="element(div)"/>
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:param name="moveTo" as="xs:integer"/>
<xsl:param name="board" as="element(div)*"/>

<piece-move>
<xsl:choose>
<xsl:when test="$moveFrom = $moveTo">
<xsl:message>isValid 1</xsl:message>
<xsl:attribute name="is-valid" select="'no'"/>
<xsl:attribute name="description" select="'not moved'"/>
</xsl:when>
<xsl:when test="$board[$moveFrom][self::empty]">
<xsl:attribute name="is-valid" select="'no'"/>
<xsl:attribute name="description" select="'no piece at start position'"/>
<xsl:message>isValid 2</xsl:message>
</xsl:when>
<xsl:when test="not($board[$moveTo][div/@data-piece eq 'empty' or div/@data-colour != $piece/@data-colour])">
<xsl:message>isValid 3: <xsl:value-of select="count($board)"/>
</xsl:message>
<xsl:attribute name="is-valid" select="'no'"/>
<xsl:attribute name="description" select="'target sqare is occupied by your own colour'"/>
<xsl:sequence select="false()"/>

</xsl:when>
<xsl:otherwise>
<xsl:message>isValid 4</xsl:message>
<!-- 
piece-specific logic
possible attributes:
is-valid = [yes|no]
enpassant-pos = position of pawn to 'take'
old-rook, new-rook = position to move rook from and to after castline
castling-colour = white/black
e.g
<move-test is-valid="yes"
enpassant-pos="45"
old-rook="1"
new-rook="4"
castling-colour="white"/>
 -->
<xsl:variable name="move-test" as="element(move-test)">
<xsl:apply-templates select="$piece" mode="is-valid-move">
<xsl:with-param name="moveFrom" select="$moveFrom"/>
<xsl:with-param name="moveTo" select="$moveTo"/>
<xsl:with-param name="board" select="$board"/>
</xsl:apply-templates>
</xsl:variable>
<xsl:copy-of select="$move-test/@*"/>
<!--<xsl:choose>
<xsl:when test="$b"><xsl:sequence select="$b"/></xsl:when>
<xsl:otherwise>
<xsl:call-template name="make-move">
<xsl:with-param name="moveFrom" select="$moveFrom" />
<xsl:with-param name="moveTo" select="$moveTo" />
<xsl:with-param name="board" select="f:get-board()" />
</xsl:call-template>
</xsl:otherwise>
</xsl:choose>-->
<!--<xsl:sequence select="if ($b) then not(f:inCheck($board[$moveFrom]/@data-colour, f:make-move($moveFrom, $moveTo, $board))"/>-->
</xsl:otherwise>
</xsl:choose>
</piece-move>
</xsl:function>

<xsl:function name="f:row" as="xs:integer">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:value-of select="(floor((64 - $moveFrom) div 8))+1"/>
</xsl:function>

<xsl:function name="f:column" as="xs:integer">
<xsl:param name="moveFrom" as="xs:integer"/>
<xsl:value-of select="(($moveFrom - 1) mod 8)+1"/>
</xsl:function>

<xsl:function name="f:square" as="xs:integer?">
<xsl:param name="row" as="xs:integer"/>
<xsl:param name="column" as="xs:integer"/>
<!-- on counting squares, row 8 becomes row 1: -->
<xsl:variable name="r" as="xs:integer"
select="abs(9 - $row)"/>
<xsl:variable name="c" as="xs:integer"
select="$column"/>
<xsl:sequence select="if ($r = 1 to 8 and $c = 1 to 8) then (($r - 1) * 8 + $c) else ()"/>
</xsl:function>

<!-- determine whether the $colour king is in check -->

<xsl:function name="f:inCheck" as="xs:boolean">
<xsl:param name="colour" as="xs:string"/>
<xsl:variable name="board-div" as="element(div)"
select="id('board', ixsl:page())"/>
<xsl:variable name="board" as="element(div)+"
select="$board-div/div"/>

<xsl:variable name="kingPiece" as="element(div)"
select="$board/div[
@data-piece eq 'king'
and @data-colour eq $colour]"/>

<xsl:variable name="kingPos"
select="count($kingPiece/parent::div/preceding-sibling::div) + 1"/>

<xsl:sequence select="
some $s in 1 to 64 satisfies
  $board[$s]/div/(@data-piece != 'empty' and @data-colour != $colour) and
  f:test(
  f:isValidMove($board[$s]/div, $s, $kingPos, $board)
  )"/>
</xsl:function>

<xsl:function name="f:test" as="xs:boolean">
<xsl:param name="piece-move" as="element(piece-move)"/>
<xsl:sequence select="$piece-move/@is-valid eq 'yes'"/>
</xsl:function>    


   


</xsl:transform>
