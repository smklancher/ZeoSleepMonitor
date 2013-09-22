<?php 

include_once 'class_progressbar.php';
include_once 'class_zeocsv.php';

$csv=new ZeoCSV('zeodata.csv');
?>

<table>
<tr>
<td>Sleep Stage</td><td>Color</td>
</tr>
<tr>
<td>Awake</td><td bgcolor="#D05827">
</tr>
<tr>
<td>Light</td><td bgcolor="#99989B">
</tr>
<tr>
<td>REM</td><td bgcolor="#29A639">
</tr>
<tr>
<td>Deep</td><td bgcolor="#00552A">
</tr>
</table>

<?php
$csv->displaygraphs(9,11);

?>