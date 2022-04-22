function bold()
{
	var text = replaceSelectionText("[B]", "[/B]");
}
function italic()
{
	var text = replaceSelectionText("[i]", "[/i]");
}
function underline()
{
	var text = replaceSelectionText("[u]", "[/u]");
}

function replaceSelectionText(prefix, suffix) {
	var textElement = document.getElementById("cn-text");
	var text = textElement.value;

	var selectionStart = textElement.selectionStart;
	var selectionEnd = textElement.selectionEnd;

	var selectedText = text.substring(selectionStart, selectionEnd)

	var outstr = text.substr(0,selectionStart)+prefix+selectedText+suffix+text.substr(selectionEnd);
	textElement.value = outstr;

	//return selectedText;
}

function toggleElement(element) {
	if (element.style.display === "none") {
		element.style.display = "block";
	} else {
		element.style.display = "none";
	}
}