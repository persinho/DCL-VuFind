<?php
require_once dirname(__FILE__).'/../../vufind/classes/Utils/ArrayUtils.php';

class ArrayUtilsTests extends PHPUnit_Framework_TestCase
{
	
	private $service;
	
	
	/** method isArray
	 * when parameterIsNotArray
	 * should returnFalse
	 * @dataProvider DP_isArray_parameterIsNotArray
	 */
	public function test_isArray_NumArgsAreCorrect_returnFalse($args)
	{
		$expected = false;
		$actual = ArrayUtils::isArray($args);
		$this->assertEquals($expected,$actual);
	}
	
	public function DP_isArray_parameterIsNotArray()
	{
		return array(
				array("string"),
				array(1),
				array(new stdClass()),
				array(true),
				array(NULL)
		);
	}
}
?>