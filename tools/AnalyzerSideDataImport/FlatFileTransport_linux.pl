use File::Copy;
use File::stat;
use Time::localtime;
use File::Basename;
use Cwd;

my $timeTagFile1 = 'pijdljkanel_Do_not_remove.ff';
# my $timeTagFile2 = 'uaoiajeehkh_Do_not_remove.ff';



sub updateQueue{
    my $maxTime = 0;
    my $stagingDir = shift;
    my $queueDir = shift;
    my $timeTagFile = shift;
    
    if(not -e $stagingDir){
  	print "Not able to find directory \"" . $stagingDir . "\" for importing analyzer results\n\n";
  	return;
   }

    my $timeFile = $queueDir . '/' . $timeTagFile; 
    if(not  -e $timeFile ){
	open FILE, ">", $timeFile;    
	close FILE;
    }
    
    my $lastUpdateTime = stat($timeFile)->mtime;
    my @files = <$stagingDir/*.*>; 
    
    foreach $file (@files) {
	my $modTime = stat($file)->mtime;
	if( $modTime > $lastUpdateTime && -T $file ){
	    $maxTime = $modTime if $modTime > $maxTime; 
	    copy($file, $queueDir . '/' . basename($file) );
	}
	
    }
    
    utime $maxTime, $maxTime, $timeFile if $maxTime > 0;
}


sub sendToServer{
    my $queueDir = shift;
    my $upLoadtargetURL    = shift;
    my $upLoadUserName = shift;
    my $upLoadPassword = shift;
    
    my $maxRetryCount = 2;
    my $curlExe = '/usr/bin/curl'; 
    
    my @files = <$queueDir/*.*>; 
    
    foreach $file (@files) { 
	next if basename($file) eq $timeTagFile1 ||  basename($file) eq $timeTagFile2;
	my $command = $curlExe . ' -k --user ' . $upLoadUserName . ':' . $upLoadPassword
	    . ' --url ' . $upLoadtargetURL 
	    . ' --form file=@' . '"' .$file . '"' ;
	
	
	print $command . "\n";
	my $retryCount = 0;
	my $sendSuccess = 0; #false
	
	while ($retryCount < $maxRetryCount) {
	    my $curlReturn = `$command`;
	    my $returnStatus = $?;
	    
	    if (($returnStatus != 0) || ($curlReturn ne 'success')) {
		print "Curl had an error. Curl said \n$curlReturn\n"
		    . "Return status $returnStatus\n";
		$retryCount = $retryCount + 1;
	    } else {
		$sendSuccess = 1; #true
		last;
	    }
	    sleep 7;
	}
	
	if ($sendSuccess) {
	    #remove file from system
	    unlink( $file );
	}
    }           
}

sub printTime{
    @months = qw(Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec);
    @weekDays = qw(Sun Mon Tue Wed Thu Fri Sat Sun);

    $second = localtime->sec;
    $minute = localtime->min;
    $hour = localtime->hour;
    $month = localtime->mon;
    $year = localtime->year + 1900;;
    $dayOfWeek = localtime->wday;
    $dayOfMonth = localtime->mday;

    print "$hour:$minute:$second, $weekDays[$dayOfWeek] $months[$month] $dayOfMonth, $year\n";
}

my $upLoadtargetURL = 'https://localhost:8443/api/OpenELIS-Global/importAnalyzer';

my $stagingDir1 = ".//staging";
#my $stagingDir2 = "Y:";
my $queueDir = ".//transmissionQueue";
my $upLoadUserName = 'analyzer';
my $upLoadPassword = 'analyzer!';

print "Welcome to analyzer import\n";
 
while( 1 ){
    updateQueue( $stagingDir1, $queueDir, $timeTagFile1 );
 #   updateQueue( $stagingDir2, $queueDir, $timeTagFile2 );
    sendToServer($queueDir, $upLoadtargetURL, $upLoadUserName, $upLoadPassword);

   
   printTime(); 
 sleep 30;
}
