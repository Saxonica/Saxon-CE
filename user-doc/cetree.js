var item;
var dScroll = false;
var disableScroll = function() {
    dScroll = true;
}
var enableScroll = function() {
    dScroll = false;
}
var swapItem = function(newItem) {
    if (!(dScroll)) {
        newItem.scrollIntoView(true);
    }
    var prevItem = item;
    item = newItem;
    return prevItem
};
var scrollToElement = function(elementItem) {
    if (elementItem != null) {
        elementItem.scrollIntoView(true);
    }
};
var goback = function() {
    window.history.back();
};
var foundPaths;
var getPaths = function() {
    return foundPaths;
};
var setPaths = function(pathsIn) {
    foundPaths = pathsIn;
};
var usesClick = function() {
    return !('ontouchstart' in document.documentElement);
};
var bg = "#c9f9e5";
var findit = function(textIn) {
    if (window.find) {
        document.designMode='on';
        try {
        document.execCommand("enableObjectResizing", false, false);
        } catch(e) {
        }
        if (window.getSelection().removeAllRanges) {  // Firefox
           document.getElementById('search').disabled = true;
        }
        var sel = document.getSelection();
        sel.collapse(document.getElementById('main'), 0);
               try {
               while (window.find(textIn, false)) {
                  var aNode = window.getSelection().anchorNode;
                  aNode = aNode.nodeType == 3 ? aNode.parentNode : aNode;
                  var idName = aNode.getAttribute('ID');
                  var className = aNode.getAttribute('CLASS');
                  if (idName != 'logo' && idName != 'search' && idName != 'header' && className != 'unselectable') {
                     document.execCommand("hiliteColor", false, bg);
                  }
               }
               } catch(e) {}

        var endSel = document.getSelection();
        endSel.collapse(document.body, 0);
        if (window.getSelection().removeAllRanges) {  // Firefox
           document.getElementById('search').disabled = false;
        }
        document.designMode='off';
    } else if (document.body.createTextRange) {
        var docMain = document.getElementById('main');
        try {
        var textRange = document.body.createTextRange();
        textRange.moveToElementText(docMain);
        } catch(e) {
            window.alert(e);
        }

        while (textRange.findText(textIn)) 
        {
            textRange.execCommand("BackColor", false, bg);
            textRange.collapse(false);
        }
    }
};

