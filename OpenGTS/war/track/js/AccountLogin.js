var loginFormName = "Login";
var localeComboBoxName = "locale";
var newaccountFormName = "NewAcct";

var localeFieldName = localeComboBoxName;

function onChangeLocale() 
{
	var selLocale = document.forms[loginFormName].elements.namedItem(localeComboBoxName).value;
	if(selLocale != null) document.forms[newaccountFormName].elements.namedItem(localeFieldName).value = selLocale;
	else document.forms[newaccountFormName].elements.namedItem(localeFieldName).value = "";
}

function submitNewAccount()
{
	var newaccountForm = document.forms[newaccountFormName];
	newaccountForm.submit();
}