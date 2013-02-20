/*
* Transform XML using Saxon.run method
*/
var transform = function (command) {
    command.method = "transformToDocument";
    var proc = Saxon.run(command);
    return proc.getResultDocument('');
};

var update = function (command) {
    Saxon.run(command);
};

/*
* Create command object from XSLT name/value pair sequence
*/
var makeCommand = function (props) {
    cmd = {};
    for (x = 0; x < props.length; x += 2) {
        if (props[x + 1] != null && props[x + 1].length != 0) {
            cmd[props[x]] = props[x + 1];
        }
    }
    return cmd;
};
// utitlity function for diagnostics
var showCommand = function (cmd) {
    var r = "";
for (var prop in cmd) {
        if (cmd.hasOwnProperty(prop))
            r += prop + ": " + cmd[prop] + "\n";
}
    window.alert(r);

};

var parseXML = function (text) {
    return Saxon.parseXML(text);
}

var serializeXML = function (xmlDoc) {
    return Saxon.serializeXML(xmlDoc);
}

var testObject = {};

var includeJS = function(scriptPath)
{
var scriptNode = document.createElement('SCRIPT');
scriptNode.type = 'text/javascript';
scriptNode.src = scriptPath;

    var headNode = document.getElementById('scripts');
headNode.appendChild(scriptNode);

}

