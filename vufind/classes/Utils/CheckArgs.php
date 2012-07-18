<?php
require_once dirname(__FILE__).'/ArrayUtils.php';

interface ICheckArgs{}

class CheckArgs implements ICheckArgs
{
	
	public function isValidNumArgs($args,$numArgs)
	{
		if (!ArrayUtils::isArray($args))
		{
			throw new InvalidArgumentException("The argument must be an Array");
		}
		
		if($numArgs != count($args))
		{
			return false;
		}
		return true;
	}
		
}
?>