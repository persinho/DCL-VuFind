<?php
require_once dirname(__FILE__).'/../../vufind/classes/Utils/CheckArgs.php';

class CheckArgsTests extends PHPUnit_Framework_TestCase
{
	
	private $service;
	
	public function setUp()
	{
		$this->service = new CheckArgs();
		parent::setUp();		
	}
	

   /** method isValidNumArgs
	* when parameterIsNotArray
	* should throw
	* @expectedException InvalidArgumentException
	*/
	public function test_isValidNumArgs_NumArgsAreCorrect_throw()
	{
		$args = 1;
		$this->service->isValidNumArgs($args,1);
	}

	/**
	* method isValidNumArgs
	* when numberIsNotCorrect
	* should returnFalse
	*/
	public function test_isValidNumArgs_numberIsNotCorrect_returnFalse()
	{
		$expected = false;
		$args = array();
		$actual = $this->service->isValidNumArgs($args,2);
		$this->assertFalse($actual);
	}
	
}
?>