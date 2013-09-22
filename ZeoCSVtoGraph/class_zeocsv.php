<?php 

include_once 'class_progressbar.php';

/**
 * Examine sleep data from a Zeo csv file
 * Zeo CSV documentation is here: http://mysleep.myzeo.com/export/Export%20Data%20Help%20Sheet.pdf
 * 
 * @package Zeo
 * @author Stephen Klancher <smknight@gmail.com>
 */ 
class ZeoCSV
{
	private $data;
	
	/**
	 * Load data from csv file
	 * @param string $file csv file to load
	 */
	function __construct($file)
	{
		//get the string data from file
		$filedata=file_get_contents($file);
		
		//conver to associative array
		$this->data=self::csv_to_array($filedata);
		
		
	}
	
	
	/**
	 * Return a night of data corresponding to a date in M/D/YYYY format
	 * @param string $date M/D/YYYY
	 * @return Array night of zeo data
	 */
	public function night_by_date($date)
	{
		foreach($this->data as $night){
			if($night['Sleep Date']){
				return $night;
			}
		}
	}
	
	
	
	/**
	 * Display graphs as embedded images.  Graphs are noon to noon unless parameters are provided. Results undefined if bounds are narrower than data.
	 * @param int $startHourPM earliest hour to show in graphs
	 * @param int $endHourPM end bound of graphs
	 */
	public function displaygraphs($startHourPM=0,$endHourAM=0)
	{
		foreach($this->data as $night){
			
			$nightstart=strtotime($night['Start of Night']);
			$nightend=strtotime($night['End of Night']);
			
			//find noon of that day
			$noon=self::previous_noon($nightstart);
			
			//start at 9pm (adjust this if you have data earlier than this)
			//The right way to do this would be to actually check the data upfront and set the offset automatically
			$startoffset=60 * 60 * $startHourPM;
			$starttime=$noon + $startoffset;
			
			//end data at noon
			$endoffset=60*60*(12-$endHourAM);
			
			//find out how many intervals are needed to pad the data from noon
			$intervalseconds=30;
			$pad_needed=round(($nightstart-$starttime)/$intervalseconds);
			$pad=str_repeat('0 ',$pad_needed);
			
			//put the padded data in an array for progressbar
			$sleepdata=explode(' ',$pad . $night['Detailed Sleep Graph']);
			
			//pad the array for the total number of hours
			$sleepdata=array_pad($sleepdata,(60*60*24-$startoffset-$endoffset)/$intervalseconds,0);
			
			//print((round($nightend-$nightstart)/30) . '-'. count($sleepdata).'<br>');
			$progressbar=new ProgressBar($sleepdata,1000,50,ProgressBar::MODE_INTEGER,'zeo');
			print($night['Start of Night'].' '.$progressbar->embedded_imagetag().$night['End of Night'].'<br>');
		}
	}
	
	/**
	 * Find the previous noon from a timestamp
	 * @param int $time unix timestamp
	 * @return int unix timestamp of the previous noon
	 */
	private static function previous_noon($time)
	{
		$parts=getdate($time);
		
		if($parts['hours']<12){
			//if it is past midnight we want the previous day (mktime rolls over to the previous month if day is 0)
			return mktime(12,0,0,$parts['mon'],$parts['mday']-1,$parts['year']);
		}else{
			//otherwise use the current day
			return mktime(12,0,0,$parts['mon'],$parts['mday'],$parts['year']);
		}
	}
	

	/**
	 * Convert csv to associative array
	 * //http://www.php.net/manual/en/function.str-getcsv.php#104558
	 */
	private static function csv_to_array($input, $delimiter=',') 
	{ 
		$header = null; 
		$data = array(); 
		$csvData = str_getcsv($input, "\n"); 
		
		foreach($csvData as $csvLine){ 
			if(is_null($header)) $header = explode($delimiter, $csvLine); 
			else{ 
				
				$items = explode($delimiter, $csvLine); 
				
				for($n = 0, $m = count($header); $n < $m; $n++){ 
					$prepareData[$header[$n]] = $items[$n]; 
				} 
				
				$data[] = $prepareData; 
			} 
		} 
		
		return $data; 
	} 

}

?>