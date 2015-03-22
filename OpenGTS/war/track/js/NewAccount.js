var timerId = 0;
var timerActive = 0;
var timeElapsed = 0;
var timeAction = 5;

function NewAccountOnLoad()
{
	var btn = document.getElementsByName('e_submit');
	btn[0].disabled = true;
	timerId = setInterval ("everySecond()", 1000);
	timerActive = 1;
	addTimerInput();
}

function NewAccountOnUnload()
{
	if(timerActive > 0) clearInterval(timerId);
}

function everySecond()
{
	timeElapsed++;
	UpdateTimerFormInput();
	if(timeElapsed > timeAction) { addHiddenInput(); clearInterval(timerId); timerActive = 0; } 
}

function UpdateTimerFormInput()
{
	var txtinput = document.getElementById('idetimer');
	txtinput.value = timeElapsed;
}

function addTimerInput()
{
	var hdndiv = document.getElementById('hdndiv');
	hdndiv.style.visibility = 'hidden';
	hdndiv.innerHTML += 'It is just a timer <input type="text" name="e_timer" id="idetimer" value="" /><br>';
}

function addHiddenInput()
{
	var hdndiv = document.getElementById('hdndiv');
	var btn = document.getElementsByName('e_submit');
	hdndiv.innerHTML += 'You should not see it <input type="text" name="e_hdn" id="idehdn" value=" " /><br>';
	UpdateTimerFormInput();
	btn[0].disabled = false;
	
}
