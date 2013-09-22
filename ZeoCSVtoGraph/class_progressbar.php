<?php 


/**
 * Create a progress bar image
 * 
 * @package MediaMoment
 * @author Stephen Klancher <smknight@gmail.com>
 */ 
class ProgressBar
{
	public $image;
	public $colors;
	public $slices;
	public $data;
	
	public $width;
	public $height;
	private $ratio;
	private $themename;
	private $mode;
	
	public $outline=true;
	public $outline_bit_threshhold=0;
	
	const UNINITIALIZED=-1;
	const OFF=0;
	const ON=1;
	const PARTIAL=2;
	const HIGHLIGHTED=3;
	
	//In the default mode, we use (essentially) boolean data and use a separate color for slices partially on, partially off
	const MODE_BOOLEAN_PARTIAL=0;
	//In this mode we map any number of integers to any number of colors
	const MODE_INTEGER=1;
	
	//theme name => array(mapped integer => array(r,g,b))
	private static $themes=null;
	
	/**
	 * create the progress bar etc.
	 * @param $data Array zero based array of boolean values (actually 0/1/2=on/off/highlighted) representing data completed or not
	 * @
	 */
	public function __construct($data,$width,$height,$mode=self::MODE_BOOLEAN_PARTIAL,$theme='default_boolean')
	{
		//initialize default themes
		if(is_null(self::$themes)){
			self::intialize_themes();
		}
		
		$this->data=$data;
		$this->width=$width;
		$this->height=$height;
		$this->themename=$theme;
		$this->mode=$mode;
		
		//PageViewLog::log('ProgressBar: Creating slices.');
		$this->create_slices();
		//PageViewLog::log('ProgressBar: Finished slices, creating image.');
		$this->create_image();
		//PageViewLog::log('ProgressBar: Finished image.');
		
	}
	
	/**
	 * create the static themes in format: array(theme name => array(mapped integer => array(r,g,b)))
	 * 
	 */
	private static function intialize_themes()
	{
		
		$lightgray=array(150,150,150);
		$gray=array(204,204,204);
		$white=array(255,255,255);
		$black=array(0,0,0);
		$blue=array(0,0,255);
		$lightblue=array(30,144,255);
		
		$zeo_deep=array(0,85,42);		//00552A
		$zeo_light=array(153,152,155);	//99989B
		$zeo_rem=array(41,166,57);		//29A639
		$zeo_wake=array(208,88,39);		//D05827
		
		
		self::$themes=array();
		
		//default for movie site
		self::$themes['default_boolean']=array(
			self::UNINITIALIZED=>$lightgray,
			self::OFF=>$lightgray,
			self::ON=>$blue,
			self::PARTIAL=>$lightblue
		);
		
		//match zeo sleep graphs
		self::$themes['zeo']=array(
			self::UNINITIALIZED=>$white,
			0=>$white,
			1=>$zeo_wake,
			2=>$zeo_rem,
			3=>$zeo_light,
			4=>$zeo_deep,
			4=>$zeo_lightdeep
		);
	}
	
	/**
	 * create the color resources in the image from the RGB values in the theme
	 * 
	 */
	private function load_theme()
	{
		//either we already have a theme or dynamically create a set of colors
		if(array_key_exists($this->themename,self::$themes)){
			$theme=self::$themes[$this->themename];
		}else{
			//dynamically create theme
			trigger_error('ProgressBar: Theme named '.$this->themename.' does not exist and dynamic theme not implemented.',E_USER_ERROR);
			
			//something like this...
			for($i=0;$i<=25;$i++){
				//$c[$i]=$this->random_color($image);
				$c[$i]=$this->createcolor($image,$i*5,$i*10,255-($i * 20));
			}
		}
		
		$this->colors=array();
		
		//create each color from the theme in the image
		foreach($theme as $number=>$color){
			$this->colors[$number]=$this->createcolor($this->image,$color[0],$color[1],$color[2]);
		}
	}
	
	
	private function create_slices()
	{
		//set ratio of slices to data
		if(count($this->data)>0){
			$this->ratio=$this->width/count($this->data);
		}
		
		//representing each slice of the image: -1=unset, 0=not filled, 1=partial, 2=filled
		$slices=array_fill(0,$this->width,self::UNINITIALIZED);
		
		//PageViewLog::log('Creating slices ('.count($slices).') for progress bar data ('.count($this->data).')');
		
		//for each bit of data
		for($bit=0;$bit<count($this->data);$bit++){
			//find out which slices are affected by this bit of data
			$startslice=round(($bit)*$this->ratio);
			$endslice=round(($bit+1)*$this->ratio);
			//PageViewLog::log('bit '.$bit.'='.$this->data[$bit].' will affect slice '.$startslice.' through '.$endslice);
			
			//for each slice that would be touched by this bit of data
			for($slice=$startslice;$slice<=$endslice;$slice++){
				
				if($this->mode==self::MODE_INTEGER){
					//integer mode is a one to one map
					$slices[$slice]=$this->data[$bit];
				}else{
					//boolean partial mode means we have to detect if something should be partial filled
					
					//decide what to do based on the current status of the slice
					switch($slices[$slice]) {
						case self::UNINITIALIZED:
							//if uninitialized, set the slice to the value of the bit
							$slices[$slice]=$this->data[$bit];
							//PageViewLog::log('Bit '.$bit.' set slice '.$slice.' to '.$slices[$slice]);
							break;
							
						case self::HIGHLIGHTED:
						case self::PARTIAL:
							// if it is already highlighted or partial keep it that way
							//PageViewLog::log('already-'.self::PARTIAL.'-no-change, slice '.$slice.'='.$slices[$slice].', bit '.$bit.'='.$this->data[$bit]);
							break;
						
						case self::ON:
						case self::OFF:
							//if the slice is already set differently than the bit, then change to partial
							if($slices[$slice] != $this->data[$bit]){
								$slices[$slice]=self::PARTIAL;
								//PageViewLog::log('Overlap-change-from-'.$slices[$slice].'-to-partial, slice '.$slice.'='.$slices[$slice].', bit '.$bit.'='.$this->data[$bit]);
							}else{
								//the slice is already the same as the bit
								//PageViewLog::log('Bit '.$bit.' - slice '.$slice.' already set to '.$slices[$slice]);
							}
							break;
					}
					
				}
				
			}
		}
		
		$this->slices=$slices;
	}
	
	public function create_image()
	{
		//create the image
		$this->image= @imagecreatetruecolor($this->width,$this->height)
			  or trigger_error('Cannot Initialize new GD image stream',E_USER_ERROR);
		
		//create the colors
		$this->load_theme();
		
		//start with a bg color from the theme
		imagefill($this->image,0,0,$this->colors[self::UNINITIALIZED]);
		
		//loop through slices to create image
		for($i=0;$i<$this->width;$i++){
			//number assigned to the current slice
			$s=$this->slices[$i];
			
			//color assigned to the current slice
			$c=$this->colors[$s];
			
			//draw the slice
			imageline($this->image,$i,0,$i,$this->height-1,$c);
			
			//PageViewLog::log('Drawing slice '.$i.'='.$this->slices[$i]);
		}
		
		//outline
		if($this->outline){
			imagerectangle($this->image,0,0,$this->width-1,$this->height-1,$this->colors['black']);
		}
		
		//bit divisions
		if($this->outline_bit_threshhold>3 && $this->ratio>=$this->outline_bit_threshhold){
			for($bit=0;$bit<count($this->data);$bit++){
				imageline($this->image,round(($bit+1)*$this->ratio),0,
						  round(($bit+1)*$this->ratio),$this->height-1,$this->colors['black']);
			}
		}
	}
	
	
	public function embedded_imagetag()
	{
		$file=$_SERVER['DOCUMENT_ROOT'].'/'.uniqid(rand(),true).'.png';
		
		//write the image out
		imagepng($this->image,$file);
		
		//read it in as a string
		$imgstr=file_get_contents($file);
		
		//delete file
		unlink($file);
		
		// base64 encode the binary data, then break it
		// into chunks according to RFC 2045 semantics
		$base64 = chunk_split(base64_encode($imgstr));
		
		$tag='<img width="'.$this->width.'" height="'.$this->height.'" src="data:image/png;base64,'.$base64.'" alt="Progress Bar">';
		
		return $tag;
	}
	
	
	
	public function imagetag($file)
	{
		//write the image out
		imagepng($this->image,$file);
		
		$tag='<img width="'.$this->width.'" height="'.$this->height.'" src="'.self::urlFromFile($file).'" alt="Progress Bar">';
		
		return $tag;
	}
	
	
	/**
	 * Returns url usable regardless of domain/subdomain by removing DOCUMENT_ROOT from the full file path
	 * 
	 * @param string $file full file path
	 * @return string relative url (relative in the sense that smknight.com/x/y.php is /x/y.php)
	 */
	public static function urlFromFile($file) {
		return str_replace($_SERVER["DOCUMENT_ROOT"],'',$file);
	}
	
	
	
	
	
	private function random_color($image)
	{
		//assign random rgb values
		$c1 = mt_rand(50,200); //r(ed)
		$c2 = mt_rand(50,200); //g(reen)
		$c3 = mt_rand(50,200); //b(lue)
		return $this->createcolor($image,$c1,$c2,$c3);
	}
	
	//http://us2.php.net/manual/en/function.imagecolorallocate.php#94785
	private function createcolor($pic,$c1,$c2,$c3) {
		//get color from palette
		$color = imagecolorexact($pic, $c1, $c2, $c3);
		if($color==-1) {
			//color does not exist...
			//test if we have used up palette
			if(imagecolorstotal($pic)>=255) {
				//palette used up; pick closest assigned color
				$color = imagecolorclosest($pic, $c1, $c2, $c3);
			} else {
				//palette NOT used up; assign new color
				$color = imagecolorallocate($pic, $c1, $c2, $c3);
			}
		}
		return $color;
	}
}

?>