<?php
require_once dirname(__FILE__).'/../../web/services/EcontentRecord/Reindex.php';
require_once dirname(__FILE__).'/../../web/sys/ConfigArray.php';
require_once dirname(__FILE__).'/../../classes/Utils/CheckArgs.php';
global $servername;


$checkArgs = new CheckArgs();
try {
	if (!$checkArgs->isValidNumArgs($argv, 2))
	{
		echo "The argument Server Name is mandatory";
		die(0);
	}
} catch (Exception $e) {
	echo "Argument not valid";
	die(0);
}

$_SERVER['SERVER_NAME'] = $argv[1];
$configArray = readConfig();
define('DB_DATAOBJECT_NO_OVERLOAD', 0);
$options =& PEAR::getStaticProperty('DB_DataObject', 'options');
print_r($options);
$options = $configArray['Database'];
//print_r($options);
echo 'HOLA';
?>