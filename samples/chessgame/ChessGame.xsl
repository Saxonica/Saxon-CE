<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
 xmlns:prop="http://saxonica.com/ns/html-property"
 xmlns:style="http://saxonica.com/ns/html-style-property" xmlns:js="http://saxonica.com/ns/globalJS"
 xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0"
 xmlns:f="http://www.saxonica.com/chessGame" exclude-result-prefixes="xs prop f"
 extension-element-prefixes="ixsl prop style">

 <xsl:import href="ChessGameValidation.xsl"/>


 <xsl:variable name="a-style">
  <xsl:value-of
   select="'color:#000;
display:block;
font-size:60px;
height:60px;
position:relative;
text-decoration:none;
text-shadow:0 1px #fff;
width:60px; text-align:center;vertical-align:top'"
  />
 </xsl:variable>

 <xsl:variable name="win" select="ixsl:window()"/>

 <xsl:variable name="all-pieces" as="element()">
  <pieces>
   <div data-piece="rook" data-colour="black" data-n="R" draggable="true">
    <a style="{$a-style}">&#9820;</a>
   </div>
   <div data-piece="knight" data-colour="black" data-n="N" draggable="true">
    <a style="{$a-style}">&#9822;</a>
   </div>
   <div data-piece="bishop" data-colour="black" data-n="B" draggable="true">
    <a style="{$a-style}">&#9821;</a>
   </div>
   <div data-piece="king" data-colour="black" data-n="K" draggable="true">
    <a style="{$a-style}">&#9818;</a>
   </div>
   <div data-piece="queen" data-colour="black" data-n="Q" draggable="true">
    <a style="{$a-style}">&#9819;</a>
   </div>
   <div data-piece="pawn" data-colour="black" data-n="P" draggable="true">
    <a style="{$a-style}">&#9823;</a>
   </div>

   <div data-piece="rook" data-colour="white" data-n="r" draggable="true">
    <a style="{$a-style}">&#9814;</a>
   </div>
   <div data-piece="knight" data-colour="white" data-n="n" draggable="true">
    <a style="{$a-style}">&#9816;</a>
   </div>
   <div data-piece="bishop" data-colour="white" data-n="b" draggable="true">
    <a style="{$a-style}">&#9815;</a>
   </div>
   <div data-piece="king" data-colour="white" data-n="k" draggable="true">
    <a style="{$a-style}">&#9812;</a>
   </div>
   <div data-piece="queen" data-colour="white" data-n="q" draggable="true">
    <a style="{$a-style}">&#9813;</a>
   </div>
   <div data-piece="pawn" data-colour="white" data-n="p" draggable="true">
    <a style="{$a-style}">&#9817;</a>
   </div>
   <div data-piece="empty" data-colour="undefined" data-n="_" draggable="true">
    <a style="{$a-style}"/>
   </div>
  </pieces>
 </xsl:variable>

 <xsl:function name="f:non-piece" as="element()">
  <xsl:copy-of select="$all-pieces/div[@data-piece eq 'empty']"/>
 </xsl:function>

 <!-- 64 chars, 1 per position -->
 <xsl:variable name="initial-board" as="xs:string"
  select="'RNBQKBNRPPPPPPPP________________________________pppppppprnbqkbnr'"/>

 <xsl:variable name="squares"
  select="('a8','b8','c8','d8','e8','f8','g8', 'h8',
'a7','b7','c7','d7','e7','f7','g7', 'h7',
'a6','b6','c6','d6','e6','f6','g6', 'h6',
'a5','b5','c5','d5','e5','f5','g5', 'h5',
'a4','b4','c4','d4','e4','f4','g4', 'h4',
'a3','b3','c3','d3','e3','f3','g3', 'h3',
'a2','b2','c2','d2','e2','f2','g2', 'h2',
'a1','b1','c1','d1','e1','f1','g1', 'h1')"
  as="xs:string*"/>


 <xsl:function name="f:get-piece" as="element(div)">
  <xsl:param name="notation" as="xs:string"/>
  <xsl:param name="position" as="xs:integer"/>
  <xsl:variable name="piece-char" select="substring($notation, $position, 1)" as="xs:string"/>
  <xsl:sequence select="$all-pieces/div[@data-n eq $piece-char]"/>
 </xsl:function>

 <xsl:template match="button[@id eq 'pin-ok']" mode="ixsl:onclick">
  <xsl:variable name="pin" as="xs:string" select="ixsl:get(id('pin-input'), 'value')"/>
  <xsl:choose>
   <xsl:when test="$pin ne ''">
    <xsl:variable name="user" select="f:get-user-id(true())" as="xs:string"/>
    <xsl:variable name="response-text" select="js:authenticateUser($pin, $user)"/>
    <xsl:message>response-text <xsl:value-of select="$response-text"/></xsl:message>
    <xsl:variable name="response" select="f:get-response($response-text)"/>
    <xsl:choose>
     <xsl:when test="$response eq 'OK'">
      <xsl:call-template name="hide-pin"/>

      <xsl:variable name="moves-div" select="id('moves', ixsl:page())"/>
      <xsl:variable name="last-move-num" select="$moves-div/tr[last()]/td[1]" as="xs:integer"/>

      <!-- Submit board to twitter -->
      <xsl:sequence select="f:submit-board($last-move-num)"/>
     </xsl:when>
     <xsl:otherwise>
      <xsl:call-template name="show-pin">
       <xsl:with-param name="url" select="''"/>
       <xsl:with-param name="retry" select="'[Retry] '"/>
      </xsl:call-template>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:when>
   <xsl:otherwise>
    <xsl:call-template name="show-pin">
     <xsl:with-param name="url" select="''"/>
     <xsl:with-param name="retry" select="'[PIN empty] '"/>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>

 </xsl:template>

 <xsl:template name="show-user">
  <xsl:param name="is-black" as="xs:boolean"/>
  <xsl:result-document href="#black-label" method="replace-content">
   <xsl:value-of select="if ($is-black) then 'self' else ''"/>
  </xsl:result-document>
  <xsl:result-document href="#white-label" method="replace-content">
   <xsl:value-of select="if ($is-black) then '' else 'self'"/>
  </xsl:result-document>
  <xsl:result-document href="#colour" method="replace-content">
   <xsl:value-of select="if ($is-black) then 'Black' else 'White'"/>
  </xsl:result-document>
 <xsl:variable name="squares" select="f:get-board()" as="element(div)*"/>
  <xsl:variable name="board" as="xs:string"
   select="string-join(
   for $s in $squares return
   string($s/div/@data-n)
   ,'')"/>
  <xsl:choose>
   <xsl:when test="$is-black">
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$board"/>
     <xsl:with-param name="player-color" select="'black'"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$board"/>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template match="input[@id = 'black-user']" mode="ixsl:onclick">
  <xsl:variable name="is-white" select="js:toggleCheckbox('white-user')" as="xs:boolean"/>
  <xsl:call-template name="show-user">
   <xsl:with-param name="is-black" select="not($is-white)"/>
  </xsl:call-template>
 </xsl:template>

 <xsl:template match="input[@id eq 'white-user']" mode="ixsl:onclick">
  <xsl:variable name="is-black" select="js:toggleCheckbox('black-user')" as="xs:boolean"/>
  <xsl:call-template name="show-user">
   <xsl:with-param name="is-black" select="$is-black"/>
  </xsl:call-template>
 </xsl:template>

 <xsl:function name="f:get-user-id" as="xs:string?">
  <xsl:param name="for-self" as="xs:boolean"/>
  <xsl:variable name="white-selected" as="xs:boolean" select="js:isChecked('white-user')"/>
  <xsl:variable name="black-selected" as="xs:boolean" select="js:isChecked('black-user')"/>
  <xsl:variable name="user-id" as="xs:string?"
   select="if ($for-self and $white-selected
                            or not($for-self) and $black-selected) then
                        'user1'
                        else if ($for-self and $black-selected
                                 or not($for-self) and $white-selected) then
                        'user2'
                        else ()"/>
  <xsl:sequence
   select="if (exists($user-id)) then
                        ixsl:get(id($user-id, ixsl:page()), 'value')
                        else ()"
  />
 </xsl:function>

 <!-- required because Saxon-CE registers event on the document node only -->
 <xsl:template name="addSquareDragHandler">
  <xsl:variable name="boardDiv" select="ixsl:page()//div[@id='board']"/>
  <xsl:for-each select="$boardDiv/div">
   <xsl:variable name="squareElement" select="." as="element()"/>
   <xsl:sequence select="js:addOnDragListener($squareElement)"/>
   <xsl:if test="$squareElement/div[@data-n ne '_']">
    <xsl:variable name="pieceElement" select="$squareElement/div" as="element()"/>
    <xsl:sequence select="js:addPieceDragListener($pieceElement)"/>
   </xsl:if>
  </xsl:for-each>
 </xsl:template>


 <xsl:template match="button[@id eq 'pin-cancel']" mode="ixsl:onclick">
  <xsl:call-template name="hide-pin"/>
  <xsl:message>cancelled</xsl:message>
 </xsl:template>

 <xsl:template name="hide-pin">
  <xsl:for-each select="id('pin-form', ixsl:page())">
   <ixsl:set-attribute name="style:display" select="'none'"/>
  </xsl:for-each>
 </xsl:template>

 <xsl:template name="show-pin">
  <xsl:param name="url"/>
  <xsl:param name="retry" as="xs:string"/>
  <xsl:for-each select="id('pin-form', ixsl:page())">
   <ixsl:set-attribute name="style:display" select="'block'"/>
  </xsl:for-each>

  <xsl:choose>
   <xsl:when test="$retry ne ''">
    <xsl:result-document href="#twitter-url" method="replace-content">
     <p>1. <xsl:value-of select="$retry"/>Fetch Twitter session PIN from</p>
     <a class="link" id="twitter-url" href="{$url}" target="_blank"> twitter authentication page </a>
     <p>2. Copy the Twitter PIN displayed and enter here:</p>
    </xsl:result-document>
   </xsl:when>
   <xsl:otherwise>
    <xsl:result-document href="#twitter-url" method="replace-content">
     <p>1. Fetch Twitter session PIN from</p>
     <a class="link" id="twitter-url" href="{$url}" target="_blank"> twitter authentication page </a>
     <p>2. Copy the Twitter PIN displayed and enter here:</p>
    </xsl:result-document>
   </xsl:otherwise>
  </xsl:choose>

 </xsl:template>


 <xsl:template match="/" name="main">
  

  <xsl:result-document href="#data" method="replace-content">
   <p data-status="new">New Game</p>
  </xsl:result-document>

  <xsl:call-template name="print-board">
   <xsl:with-param name="nboard" select="$initial-board" as="xs:string"/>
  </xsl:call-template>
  <xsl:message select="string(f:pin-hidden())"/>
  <!-- Patch fix for firefox - We initially remove the attribute on the submit and fetchBtn buttons -->
  <xsl:for-each select="id('submit', ixsl:page())">
   <ixsl:remove-attribute name="disabled" />
  </xsl:for-each>
  <xsl:for-each select="id('fetchBtn', ixsl:page())">
   <ixsl:remove-attribute name="disabled"/>
  </xsl:for-each>
  
  <!-- end of patch fix -->
  <xsl:result-document href="#moves" method="replace-content">
   
  </xsl:result-document>
 </xsl:template>

 <xsl:function name="f:pin-hidden" as="xs:boolean">
  <xsl:sequence select="id('pinSpan', ixsl:page())/@style:display eq 'none'"/>
 </xsl:function>

 <!-- callback from JavaScript dragController - which updates the move input box -->
 <xsl:template match="ixsl:get($win, 'dragController')" mode="ixsl:changed">
  <xsl:variable name="piece" as="element(div)"
   select="ixsl:get(ixsl:window(), 'dragController.piece')"/>
  <xsl:if test="id('status', ixsl:page())/@data-status!='update-status'">
   <!-- and (id('status', ixsl:page())/@data-status='new' and $piece/@data-colour='white') -->
   <xsl:message>piece: <xsl:value-of select="$piece/@data-piece, 'place: ', $piece/parent::div/@id"
    /></xsl:message>
   <xsl:for-each select="ixsl:page()">
    <xsl:call-template name="begin-move">
     <xsl:with-param name="for-self" select="true()"/>
     <!-- must set piece from context here, because it will already have moved from origin square -->
     <xsl:with-param name="piece" select="$piece" as="element(div)"/>
    </xsl:call-template>
   </xsl:for-each>
  </xsl:if>
 </xsl:template>

 <xsl:template name="print-board">
  <xsl:param name="nboard" as="xs:string"/>
  <xsl:param name="player-color" as="xs:string" select="'white'"/>

  <xsl:result-document href="#board" method="replace-content">
   <xsl:for-each select="0 to 7">
    <xsl:variable name="row" select="."/>

    <xsl:for-each select="0 to 7">
     <xsl:variable name="column" select="."/>
     <xsl:variable name="square" select="$row*8 + $column"/>
     <xsl:variable name="shading"
      select="if ((($row + $column) mod 2)=0)
              then '         background:#fff;
              background:-moz-linear-gradient(top, #fff, #eee);
              background:-webkit-gradient(linear,0 0, 0 100%, from(#fff), to(#eee));
              box-shadow:inset 0 0 0 1px #fff;
              -moz-box-shadow:inset 0 0 0 1px #fff;
              -webkit-box-shadow:inset 0 0 0 1px #fff;' else 'background:#ccc;
              background:-moz-linear-gradient(top, #ccc, #eee);
              background:-webkit-gradient(linear,0 0, 0 100%, from(#ccc), to(#eee));
              box-shadow:inset 0 0 10px rgba(0,0,0,.4);
              -moz-box-shadow:inset 0 0 10px rgba(0,0,0,.4);
              -webkit-box-shadow:inset 0 0 10px rgba(0,0,0,.4);'"/>
     <xsl:variable name="rowPos"
			select="if ($player-color = 'white') then (60*$row) else 420 - (60*$row)"/>
     <xsl:variable name="columnPos" select="if ($player-color = 'white') then (60*$column) else 420 - (60*$column)" />
     <xsl:variable name="styleVar"
      select="concat('position:absolute; margin:0px; height:60px; width:60px;border:0px; padding:0px; margin:0px; top:',
              ($rowPos),'px; left:',($columnPos),'px;',$shading)"/>
     <div id="{$squares[$square+1]}" style="{$styleVar}">
      <xsl:copy-of select="f:get-piece($nboard, $square + 1)"/>
     </div>
    </xsl:for-each>
   </xsl:for-each>

  </xsl:result-document>

  <!-- add drag event handlers after board is created -->
  <ixsl:schedule-action wait="20">
   <xsl:call-template name="addSquareDragHandler"/>
  </ixsl:schedule-action>
 </xsl:template>

 <xsl:template match="a">
  <xsl:copy-of select="."/>
 </xsl:template>

 <xsl:template match="div[matches(@id, '\w\d')]" mode="ixsl:onclick">

  <xsl:variable name="initial-square" select="@id"/>
  <!--  <xsl:result-document href="#data" method="replace-content">
   <p data-status="{f:get-board-status()}">
    <xsl:value-of select="$initial-square"/>
   </p>
  </xsl:result-document>-->
  <xsl:variable name="inputBxValue" select="ixsl:get(ixsl:page()//input[@id='inputBx'], 'value' )"/>

  <xsl:choose>
   <xsl:when test="string-length($inputBxValue) &lt; 3">
    <ixsl:set-property name="value" object="ixsl:page()//input[@id='inputBx']"
     select="concat($initial-square, '-')"/>
   </xsl:when>
   <xsl:when test="string-length($inputBxValue) = 3">
    <ixsl:set-property name="value" object="ixsl:page()//input[@id='inputBx']"
     select="concat($inputBxValue, $initial-square)"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:variable name="square1"
     select="ixsl:page()//div[@id= substring-before($inputBxValue, '-')]"/>
    <xsl:variable name="square2" select="ixsl:page()//div[@id= substring-after($inputBxValue, '-')]"/>
    <xsl:for-each select="$square1/div/a">
     <ixsl:set-attribute name="style:background-color" select="'transparent'"/>
    </xsl:for-each>
    <xsl:for-each select="$square2/div/a">
     <ixsl:set-attribute name="style:background-color" select="'transparent'"/>
    </xsl:for-each>
    <ixsl:set-property name="value" object="ixsl:page()//input[@id='inputBx']"
     select="concat($initial-square,'-')"/>

   </xsl:otherwise>
  </xsl:choose>
  <xsl:for-each select="./div/a">
   <ixsl:set-attribute name="style:background-color" select="'#9999FF'"/>
  </xsl:for-each>
 </xsl:template>

 <xsl:function name="f:get-board" as="element()*">
  <xsl:variable name="boardDiv" select="ixsl:page()//div[@id='board']"/>
  <xsl:sequence select="$boardDiv/div"/>
 </xsl:function>

 <xsl:template name="print-message">
  <xsl:param name="input"/>
  <xsl:result-document href="#data" method="replace-content"> message:<xsl:value-of select="$input"
   />
  </xsl:result-document>
 </xsl:template>

 <xsl:template name="make-move">
  <xsl:param name="piece" as="element(div)"/>
  <xsl:param name="moveFrom" as="xs:integer"/>
  <xsl:param name="moveTo" as="xs:integer"/>
  <xsl:param name="for-self" as="xs:boolean"/>

  <xsl:variable name="board" select="id('board')/div" as="element(div)+"/>
  <!-- options: initial, next -->
  <!-- black or white -->
  <!-- integer in range 0..63 -->

  <!-- <xsl:result-document href="#data" method="append-content">
make move:<xsl:copy-of select="$board[$moveFrom]" />  <xsl:value-of select="f:isValidMove1($moveFrom, $moveTo, $board)"></xsl:value-of>
</xsl:result-document>-->
    <xsl:variable name="piece-move" as="element(piece-move)"
                 select="f:isValidMove($piece, $moveFrom, $moveTo, $board)"/> <xsl:choose>

   <xsl:when test="f:inCheck($piece/@data-colour)">
    <xsl:result-document href="#data" method="append-content">Move would put you in check</xsl:result-document>
   </xsl:when>
   <xsl:when test="f:test($piece-move)">
    <!-- check move is Ok, then make move -->
    <!-- <xsl:sequence
select="
for $i in 1 to 64 return
if ($i = $moveTo) then $board[$moveFrom] else 
if($i = $moveFrom) then $non-piece else $board[$i]"
/>-->
  <xsl:result-document href="#data" method="replace-content"> Game in progress </xsl:result-document>
    <xsl:apply-templates select="$board[$moveFrom]" mode="move">
     <xsl:with-param name="targetSquare" select="$moveTo" as="xs:integer"/>
     <xsl:with-param name="piece-move" as="element(piece-move)" select="$piece-move"/>
    </xsl:apply-templates>

    <xsl:variable name="user1" select="f:get-user-id(true())" as="xs:string"/>
    <xsl:variable name="user2" select="f:get-user-id(false())" as="xs:string"/>

    <!-- variable init -->
    <xsl:variable name="moves-div" select="id('moves', ixsl:page())"/>
    <xsl:variable name="last-row" select="$moves-div/tr[last()]" as="element(tr)?"/>
    <xsl:variable name="piece-colour" as="xs:string" select="$piece/@data-colour"/>
    <xsl:variable name="empty-td" as="element(td)?" select="$last-row/td[string-length(.) eq 0]"/>
    <xsl:variable name="target-td" as="element(td)?" select="$empty-td[@class eq $piece-colour]"/>
    <xsl:message>target-td-exists: <xsl:value-of select="$target-td, exists($target-td)"
     /></xsl:message>

    <xsl:variable name="last-td-number" as="xs:integer"
     select="if ($last-row) then
     xs:integer($last-row/td[1])
     else 0"/>

    <xsl:variable name="move-num" as="xs:integer"
     select="if (exists($target-td))
     then $last-td-number
     else $last-td-number + 1"/>

    <xsl:variable name="add-new-row" as="xs:boolean" select="empty($last-row) or empty($target-td)"/>

    <!-- end variable init -->

    <!-- Submit board to twitter -->
    <xsl:if test="$for-self">
     <xsl:choose>
      <xsl:when test="$user1 ne '' and $user2 ne ''">
       <xsl:sequence select="f:submit-board($move-num)"/>
      </xsl:when>
      <xsl:otherwise/>
     </xsl:choose>
    </xsl:if>

    <xsl:variable name="move" select="concat($moveFrom, '-', $moveTo)"/>

    <xsl:sequence
     select="f:update-moves-table(
      $move, 
      $piece-colour,
      $target-td, $move-num,
      $add-new-row, false())"/>

   </xsl:when>
   <xsl:otherwise>
    <xsl:result-document href="#data" method="append-content"> move not made </xsl:result-document>
    <!-- TODO: return error -->
    <!--  <xsl:apply-templates select="$board[$moveFrom]" mode="move">
<xsl:with-param name="targetSquare" select="$moveTo" as="xs:integer"/>
</xsl:apply-templates>
<xsl:message>move not valid</xsl:message>-->
   </xsl:otherwise>

  </xsl:choose>

 </xsl:template>


 <xsl:function name="f:submit-board">
  <xsl:param name="move-num" as="xs:integer"/>
  <ixsl:schedule-action wait="20">
   <xsl:call-template name="serialize-board">
    <xsl:with-param name="move-num" select="$move-num"/>
   </xsl:call-template>
  </ixsl:schedule-action>
 </xsl:function>

 <xsl:function name="f:input-value">
  <xsl:param name="id" as="xs:string"/>
  <xsl:sequence select="ixsl:get(id($id, ixsl:page()), 'value')"/>
 </xsl:function>

 <xsl:template name="serialize-board">
  <xsl:param name="move-num" as="xs:integer"/>

  <xsl:variable name="squares" select="f:get-board()" as="element(div)*"/>
  <xsl:variable name="board" as="xs:string"
   select="string-join(
                for $s in $squares return
                string($s/div/@data-n)
                ,'')"/>
  <xsl:variable name="rows" as="xs:string+"
   select="for $i in 0 to 7 return
                        substring($board, ($i*8) + 1, 8)"/>
  <xsl:message>squares: <xsl:sequence select="string-join(('', $rows), '&#10;')"/>
  </xsl:message>
  <!-- Send move to twitter -->
  <xsl:variable name="user" select="f:get-user-id(true())" as="xs:string"/>
  <xsl:variable name="opponent" select="f:get-user-id(false())" as="xs:string"/>
  <xsl:variable name="move" select="f:input-value('inputBx')" as="xs:string"/>

  <xsl:variable name="current-date" select="current-dateTime()" as="xs:dateTime"/>
  <xsl:variable name="new-marker"
   select="concat(string(day-from-dateTime($current-date)), string(minutes-from-dateTime($current-date)), string(seconds-from-dateTime($current-date)))"
   as="xs:string"/>
  <xsl:variable name="board-is-new" select="f:get-board-status() eq 'new'"/>

  <xsl:variable name="status-text"
   select="concat('@', $opponent, ' ', $board,' ',
                        $move, ' p:', $move-num , ' ', $new-marker)"/>

  <xsl:message>serialize-board</xsl:message>
  <xsl:message select="$new-marker"/>
  <xsl:variable name="response-text"
   select="if ($board-is-new) then
             js:updateStatus($user, concat('#sxnchess:', $new-marker))
             else
             js:updateStatus($user, $status-text)
     "/>
  <xsl:variable name="response" select="f:get-response($response-text)"/>
  <xsl:choose>
   <xsl:when test="$response eq 'OK'">
    <xsl:if test="$board-is-new">
     <xsl:variable name="new-response" select="f:get-response(js:updateStatus($user, $status-text))"/>
     <xsl:sequence
      select="if ($new-response eq 'OK') then () 
        else concat('Error submitting move:', $new-response)"
     />
    </xsl:if>
    <!-- TODO:  -->
    <xsl:sequence select="f:set-board-status('update-sent')"/>
    <!-- <xsl:result-document href="#data" method="replace-content">
      <div> Move sent, wait for opponent's move... </div>
      </xsl:result-document>-->
   </xsl:when>
   <xsl:otherwise>
    <xsl:call-template name="show-pin">
     <xsl:with-param name="url" select="$response"/>
     <xsl:with-param name="retry" select="''"/>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:function name="f:get-board-status" as="xs:string">
  <xsl:sequence select="(id('status', ixsl:page())/@data-status, '')[1]"/>
 </xsl:function>

 <xsl:function name="f:set-board-status" as="xs:string?">
  <xsl:param name="value" as="xs:string"/>
  <xsl:message>set-board-status <xsl:value-of select="$value"/></xsl:message>
  <xsl:choose>
   <xsl:when test="$value='update-sent'">
    <xsl:for-each select="id('submit', ixsl:page())">
     <ixsl:set-attribute name="disabled" select="true"/>
    </xsl:for-each>
    <xsl:for-each select="id('fetchBtn', ixsl:page())">
     <ixsl:remove-attribute name="disabled"/>
    </xsl:for-each>
   </xsl:when>
   <xsl:when test="$value='update-received'">
    <xsl:for-each select="id('submit', ixsl:page())">
     <ixsl:remove-attribute name="disabled"/>
    </xsl:for-each>
    <xsl:for-each select="id('fetchBtn', ixsl:page())">
     <ixsl:set-attribute name="disabled" select="true"/>
    </xsl:for-each>
   </xsl:when>
  </xsl:choose>
  <xsl:for-each select="id('status', ixsl:page())">
   <ixsl:set-attribute name="data-status" select="$value"/>
  </xsl:for-each>
 </xsl:function>

 <xsl:function name="f:get-response" as="xs:string">
  <xsl:param name="text"/>
  <xsl:variable name="response-node" select="ixsl:parse-xml($text)" as="document-node()"/>
  <xsl:sequence select="$response-node/*"/>
 </xsl:function>


 <xsl:template match="input[@id='fetchBtn']" mode="ixsl:onclick">
  <xsl:variable name="result" as="element(result)" select="f:parse-last-tweet(1, false())"/>
  <!--<xsl:message>success <xsl:value-of select="exists($result/@piece-move)"/></xsl:message>-->
  <xsl:message>fetch move</xsl:message>
  <xsl:result-document href="#data" method="replace-content"> Game in progress </xsl:result-document>
  <xsl:choose>
   <xsl:when test="exists($result/success)">
    <xsl:for-each select="ixsl:page()">
     <xsl:call-template name="begin-move">
      <xsl:with-param name="for-self" select="false()"/>
      <xsl:with-param name="other-move" select="$result"/>
     </xsl:call-template>
     <xsl:sequence select="f:set-board-status('update-received')"/>
    </xsl:for-each>
   </xsl:when>
   <xsl:when test="exists($result/game-over)">
    <xsl:message>Winner of game is <xsl:value-of select="$result/@winner"></xsl:value-of></xsl:message>
    <xsl:call-template name="main" />
   </xsl:when>
   <xsl:otherwise>
    <xsl:message>invalid or no move in tweet</xsl:message>
   </xsl:otherwise>
  </xsl:choose>

 </xsl:template>

 <xsl:template match="input[@id='endBtn']" mode="ixsl:onclick">
  <xsl:variable name="board-is-new" select="f:get-board-status() eq 'new'"/>
  <xsl:variable name="user" select="f:get-user-id(true())" as="xs:string"/>
  <xsl:variable name="opponent" select="f:get-user-id(false())" as="xs:string"/>
  <xsl:if test="not($board-is-new)">
   <xsl:variable name="response-text" select="js:updateStatus($user, concat('#sxnchess-end: @',$opponent,' wins. Game ended by ', $user))"/>
   <xsl:variable name="response" select="f:get-response($response-text)"/>

   <xsl:if test="$response eq 'OK'"> 
    <xsl:call-template name="main" />
   </xsl:if>
  </xsl:if>
 </xsl:template>

 <xsl:template match="input[@id='resetBtn']" mode="ixsl:onclick">
  <xsl:call-template name="main"/>
 </xsl:template>

 <xsl:template match="input[@id='restoreBtn']" mode="ixsl:onclick">
  <xsl:variable name="you" as="element(result)" select="f:parse-last-tweet(1, true())"/>
  <xsl:variable name="other" as="element(result)" select="f:parse-last-tweet(1, false())"/>
  <xsl:result-document href="#data" method="replace-content"> Game in progress </xsl:result-document>
  <xsl:choose>
   <xsl:when test="exists($you/success) and empty($other/success)">
    <xsl:sequence
     select="f:update-moves-table($you/@piece-move-as-num,
                                  f:get-color-from-tweet($you/@piece-move, $you/@board),(),
                                  1,false(), true())"/>
    <xsl:sequence select="f:set-board-status('update-sent')"/>
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$you/@board" as="xs:string"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="empty($you/success) and exists($other/success)">
    <xsl:sequence
     select="f:update-moves-table(concat($other/@peice-move-as-num),
                                 f:get-color-from-tweet($other/@piece-move, $other/@board),(),
                                 1,false(), true())"/>
    <xsl:sequence select="f:set-board-status('update-received')"/>
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$other/@board" as="xs:string"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$you/@date le $other/@date">
    <xsl:sequence
     select="f:update-moves-table($other/@piece-move-as-num,
                                  f:get-color-from-tweet($other/@piece-move, $other/@board),(),
                                  1,false(), true())"/>
    <xsl:sequence select="f:set-board-status('update-received')"/>
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$other/@board" as="xs:string"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="$other/@date le $you/@date">
    <xsl:sequence
     select="f:update-moves-table($you/@piece-move-as-num,
     f:get-color-from-tweet($you/@piece-move, $you/@board),(),
     1,false(), true())"/>
    <xsl:sequence select="f:set-board-status('update-sent')"/>
    <xsl:call-template name="print-board">
     <xsl:with-param name="nboard" select="$you/@board" as="xs:string"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <xsl:message>invalid or no board in tweet</xsl:message>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <!-- TODO -->
 <xsl:function name="f:get-color-from-tweet" as="xs:string?">
  <xsl:param name="move" as="xs:string"/>
  <xsl:param name="twitter-board" as="xs:string"/>
  <xsl:variable name="piece"
   select="substring($twitter-board, index-of($squares, substring-after($move, '-')), 1)"/>
  <xsl:sequence select="if($piece = upper-case($piece)) then 'black' else 'white'"/>
 </xsl:function>

 <xsl:function name="f:update-moves-table">
  <xsl:param name="move" as="xs:string"/>
  <xsl:param name="piece-color" as="xs:string"/>
  <xsl:param name="blank-td" as="element(td)?"/>
  <xsl:param name="move-position" as="xs:integer"/>
  <xsl:param name="add-new-row" as="xs:boolean"/>
  <xsl:param name="refresh" as="xs:boolean"/>
  <xsl:variable name="move-parts" select="tokenize($move, '-')"/>
  <xsl:variable name="move-places" as="xs:string"
   select="concat(
                $squares[xs:integer($move-parts[1])], '-', $squares[xs:integer($move-parts[2])]
                )"/>

  <xsl:variable name="white-cell" select="if($piece-color = 'white') then $move-places else ()"/>
  <xsl:variable name="black-cell" select="if($piece-color = 'black') then $move-places else ()"/>
  <xsl:message>in update moves-table</xsl:message>
  <xsl:choose>
   <xsl:when test="$refresh">
    <xsl:result-document href="#moves" method="replace-content">
     <tr>
      <td>
       <xsl:value-of select="$move-position"/>
      </td>
      <td class="white">
       <xsl:value-of select="$white-cell"/>
      </td>
      <td class="black">
       <xsl:value-of select="$black-cell"/>
      </td>
     </tr>
    </xsl:result-document>
   </xsl:when>
   <xsl:when test="$add-new-row">
    <xsl:message>table: empty($blank)</xsl:message>
    <xsl:result-document href="#moves" method="append-content">
     <tr>
      <td>
       <xsl:value-of select="$move-position"/>
      </td>
      <td class="white">
       <xsl:value-of select="$white-cell"/>
      </td>
      <td class="black">
       <xsl:value-of select="$black-cell"/>
      </td>
     </tr>
    </xsl:result-document>
   </xsl:when>
   <xsl:otherwise>
    <xsl:message>table: otherwise empty($blank)</xsl:message>
    <xsl:for-each select="$blank-td">
     <xsl:result-document href="?select=." method="replace-content">
      <xsl:value-of select="$move-places"/>
     </xsl:result-document>
    </xsl:for-each>
   </xsl:otherwise>
  </xsl:choose>
  <xsl:message>after update moves-table</xsl:message>
 </xsl:function>

 <!-- Iterate through twitter timeline of opponent to get the last valid chess move
     they sent to the user. Note: no authentication is used for this -->
 <xsl:function name="f:parse-last-tweet" as="element(result)">
  <xsl:param name="attempt" as="xs:integer"/>
  <xsl:param name="self" as="xs:boolean"/>

  <xsl:variable name="max-attempts" as="xs:integer" select="10"/>
  <xsl:message>test in parse-last-tweet</xsl:message>
  <xsl:variable name="user" select="f:get-user-id(not($self))"/>
  <xsl:variable name="opponent" select="f:get-user-id($self)"/>
  <xsl:variable name="tw-response" as="document-node()"
   select="ixsl:parse-xml(js:getTwitterTimeline($opponent))"/>

  <xsl:variable name="top-status" as="element(status)?"
   select="$tw-response/timeline/status[
   contains(tweet, concat('@',$user))][$attempt]"/>
  <xsl:variable name="top-tweet" as="element(tweet)?"
   select="if (exists($top-status))
   then $top-status/tweet
   else ()"/>


  <xsl:choose>
   <xsl:when test="exists($top-tweet)">
    <xsl:variable name="tw-parts" as="xs:string*" select="tokenize($top-tweet, '\s+')"/>
    <xsl:variable name="tw-board" as="xs:string*" select="$tw-parts[2]"/>
    <xsl:variable name="piece-move" as="xs:string*" select="$tw-parts[3]"/>
    <xsl:message>pm: <xsl:value-of select="$piece-move"/></xsl:message>
    <xsl:variable name="isEnd" select="starts-with($top-tweet,'#sxnchess-end')"/>
    <xsl:variable name="valid" select="string-length($tw-board) eq 64" as="xs:boolean"/>
    <xsl:variable name="tweet-date" select="xs:dateTime($top-status/@date)" as="xs:dateTime"/>
    <xsl:choose>
     <xsl:when test="$isEnd">
      <result winner="{$tw-parts[2]}" loser="{$tw-parts[7]}" date="{$tweet-date}">
       <game-over/>
      </result>
     </xsl:when>
     <xsl:when test="$valid">
      <xsl:message>parse-last-tweet-choose: <xsl:value-of select="$piece-move"/></xsl:message>
      <xsl:variable name="moveFrom" select="index-of($squares, substring($piece-move, 1, 2))"/>
      <xsl:variable name="moveTo" select="index-of($squares, substring-after($piece-move, '-'))"/>
      <result board="{$tw-board}" piece-move="{$piece-move}" date="{$tweet-date}"
       piece-move-as-num="{$moveFrom}-{$moveTo}">
       <success/>
      </result>
     </xsl:when>
     <xsl:when test="$attempt lt $max-attempts">
      <xsl:sequence select="f:parse-last-tweet($attempt + 1, $self)"/>
     </xsl:when>
     <xsl:otherwise>
      <result/>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:when>
   <xsl:otherwise>
    <result/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:function>

 <xsl:template match="button[@id='submit']" mode="ixsl:onclick">
  <xsl:call-template name="begin-move">
   <xsl:with-param name="for-self" select="true()"/>
  </xsl:call-template>
 </xsl:template>

 <xsl:template name="begin-move">
  <xsl:param name="for-self" as="xs:boolean"/>
  <xsl:param name="piece" select="()" as="element(div)?"/>

  <xsl:param name="other-move" as="element(result)?" select="()"/>
  <!-- TODO move for opponent -->
  <xsl:variable name="moveValue"
   select="if ($for-self) then
           id('inputBx', ixsl:page())/@prop:value
           else $other-move/@piece-move"/>
  <xsl:message>other-move: <xsl:value-of select="$other-move/@piece-move"/></xsl:message>
  <xsl:variable name="checkMove" select="matches($moveValue, '\w\d-\w\d')"/>
  <xsl:choose>
   <xsl:when test="not($checkMove)">
    <xsl:result-document href="#data" method="replace-content">
     <b>Error: Invalid move(s) please try again.</b>
    </xsl:result-document>
   </xsl:when>
   <xsl:otherwise>
    <!--    <xsl:variable name="colour" select="ixsl:page()//span[@id='colour']"/>-->
    <xsl:variable name="from" select="substring-before($moveValue, '-')"/>
    <xsl:variable name="moveFrom"
     select="if (string-length($from) = 3) 
             then index-of($squares, substring($from, 2, 2)) 
             else index-of($squares, $from)"/>
    <!-- TODO - handle from move with chess piece indicated -->

    <xsl:variable name="moveTo" select="index-of($squares, substring-after($moveValue, '-'))"/>
    <!--<xsl:result-document href="#data" method="replace-content"> MoveFrom converted:<xsl:value-of
      select="$moveFrom"/> MoveTo converted:<xsl:value-of select="$moveTo"/> row:<xsl:value-of
      select="f:row($moveTo)"/> column:<xsl:value-of select="f:column($moveTo)"/>
    </xsl:result-document> -->
    <xsl:choose>
     <xsl:when test="empty($moveFrom) or empty($moveTo)">
      <xsl:result-document href="#data" method="replace-content">
       <b>Error: Invalid move(s) please try again.</b>
      </xsl:result-document>
     </xsl:when>
     <xsl:otherwise>
      <xsl:message>making move...</xsl:message>

      <xsl:variable name="movePiece" as="element(div)"
       select="if (exists($piece)) then $piece
                            else id('board',ixsl:page())/div[$moveFrom]/div"/>

      <xsl:call-template name="make-move">
       <xsl:with-param name="piece" select="$movePiece"/>
       <xsl:with-param name="moveFrom" select="$moveFrom"/>
       <xsl:with-param name="moveTo" select="$moveTo"/>
       <xsl:with-param name="for-self" select="$for-self"/>
      </xsl:call-template>

     </xsl:otherwise>
    </xsl:choose>
   </xsl:otherwise>
  </xsl:choose>

 </xsl:template>

 <xsl:template match="div[matches(@id, '\w\d')]" mode="move">
  <xsl:param name="targetSquare"/>
  <xsl:param name="piece-move" as="element(piece-move)"/>
  <xsl:result-document href="#{$squares[$targetSquare]}" method="ixsl:replace-content">
   <xsl:copy-of select="./div"/>
  </xsl:result-document>
  <xsl:for-each select="./div/a">
   <ixsl:set-attribute name="style:background-color" select="'transparent'"/>
  </xsl:for-each>
  <xsl:for-each select="ixsl:page()//div[@id = xs:string($squares[$targetSquare])]//a">
   <ixsl:set-attribute name="style:background-color" select="'transparent'"/>
  </xsl:for-each>
  <xsl:result-document href="#{./@id}" method="ixsl:replace-content">
   <xsl:copy-of select="f:non-piece()"/>
  </xsl:result-document>
  <xsl:if test="$piece-move/@enpassant-pos">
   <xsl:result-document href="#{$squares[xs:integer($piece-move/@enpassant-pos)]}" method="ixsl:replace-content">
    <xsl:copy-of select="f:non-piece()"/>
   </xsl:result-document>
  </xsl:if>
  <!--for castling -->
  <xsl:choose>
  <xsl:when test="$piece-move/@new-rook">
   <xsl:result-document href="#{$squares[xs:integer($piece-move/@old-rook)]}" method="ixsl:replace-content">
    <xsl:copy-of select="f:non-piece()"/>
   </xsl:result-document>
   <xsl:variable name="newRook" as="xs:integer"
                 select="xs:integer($piece-move/@new-rook)"/>
   <xsl:result-document href="#{$squares[$newRook]}" method="ixsl:replace-content">
    <xsl:copy-of select="$all-pieces/div[@data-piece eq 'rook' and @data-colour eq $piece-move/@castling-colour]"/>
   </xsl:result-document>
   <ixsl:schedule-action wait="20">
    <xsl:call-template name="add-piece-listener">
     <xsl:with-param name="square-id" select="$squares[$targetSquare]"/>
     <xsl:with-param name="castling-square-id" select="$squares[$newRook]"/>
    </xsl:call-template>
   </ixsl:schedule-action>
  </xsl:when>
  <xsl:otherwise>
   <ixsl:schedule-action wait="20">
    <xsl:call-template name="add-piece-listener">
     <xsl:with-param name="square-id" select="$squares[$targetSquare]"/>
    </xsl:call-template>
   </ixsl:schedule-action>
  </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="add-piece-listener">
  <xsl:param name="square-id" as="xs:string"/>
  <xsl:param name="castling-square-id" as="xs:string?" select="()"/>
  <xsl:sequence select="js:addPieceDragListener(id($square-id, ixsl:page())/div)"/>
  <xsl:if test="exists($castling-square-id)">
   <xsl:message>castling-listener: <xsl:value-of select="$castling-square-id"></xsl:value-of></xsl:message>
   <xsl:sequence select="js:addPieceDragListener(id($castling-square-id, ixsl:page())/div)"/>
  </xsl:if>
 </xsl:template>


</xsl:transform>
