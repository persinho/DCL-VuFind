<?php

require_once dirname(__FILE__).'/Utils/ArrayTests.php';
require_once dirname(__FILE__).'/Utils/CheckArgsTests.php';

class UtilsTests
{
	public static function suite()
	{
		$suite = new PHPUnit_Framework_TestSuite('Util-Tests');

		$suite->addTestSuite('ArrayUtilsTests');
		$suite->addTestSuite('CheckArgsTests');

		return $suite;
	}
}

?>