<?php
include('authentication.php');

// Get sub-rents information
$req = $bdd->prepare('SELECT rent, subrent FROM subrent;');
$req->execute();
$subrents = [];
while ($res = $req->fetch()) {
  $subrents[$res['subrent']] = array('rent' => $res['rent']);
}
$req->closeCursor();

// Get main rents with their names and capacities
$req = $bdd->prepare('SELECT id, name, capacity FROM rent;');
$req->execute();
$rents = [];
while ($rent = $req->fetch()) {
  if ($subrents[$rent['id']]) {
    // This rent is a subrent
    $subrents[$rent['id']]['capacity'] = intval($rent['capacity']);
  }
  else {
    // This rent is not a subrent
    $rents[$rent['id']] = array('name' => $rent['name'], 
                                'capacity' => intval($rent['capacity']));
  }
}
$req->closeCursor();

switch ($_POST['statistics']) {
  /** Get rented days times capacity per months **/
  case 0:
    $rentedDays = getRentedDaysByRentStatistics($bdd, $rents, $subrents, $_POST['year'], true);
    
    // Send statistics results
    echo json_encode($rentedDays);
    break;
    
    
  /** Get rented days times capacity per year **/
  case 1:
    $savedRentedDays = json_decode(readAllFile("savedRentedDays.txt"), true);
    
    $currentYear = intval(date('Y', time()));
    $currentMonth = intval(date('m', time()));
    
    // Check if the number of rented days was computed until 2 years ago.
    // If not, compute it.
    for ($year = 2017; $year <= $currentYear - 2; $year++) {
      if (!array_key_exists($year, $savedRentedDays)) {
        // This year has not been computed before, so we compute it now.
        $rentedDays = getRentedDaysByRentStatistics($bdd, $rents, $subrents, $year, false);
        // Parse the result of the previous function to keep only relevant data
        $savedRentedDays[$year] = [];
        foreach ($rentedDays as $rentId => $rentInfo)
          $savedRentedDays[$year][$rentId] = $rentInfo['rentedDaysTimesCapacity'];
      }
    }
    // We consider rented days until 2 years ago are fix now.
    // Users can not add new booking for dates older than few months.
    // Thus, we save it in a local txt file in otder to avoid compute those values again.
    writeFile("savedRentedDays.txt", json_encode($savedRentedDays));
    
    // Then compute other numbers that can vary again.
    $rentedDaysPerYear = array('rents' => $rents, 'rentedDaysTimesCapacity' => $savedRentedDays);
    for ($year = $currentYear - 1; $year <= $currentYear + 1; $year++) {
      $rentedDays = getRentedDaysByRentStatistics($bdd, $rents, $subrents, $year, false);
      // Parse the result of the previous function to keep only relevant data
      $rentedDaysForYear = [];
      foreach ($rentedDays as $rentId => $rentInfo)
        $rentedDaysForYear[$rentId] = $rentInfo['rentedDaysTimesCapacity'];
      $rentedDaysPerYear['rentedDaysTimesCapacity'][$year] = $rentedDaysForYear;
    }
    
    // Send statistics results
    echo json_encode($rentedDaysPerYear);
    break;
}

/**
  * Calculate rented days times capacity by rent.
  * It uses the capacity of each rent and sub-rents to compute it.
  * @param $bdd The access to the database.
  * @param $rents List of all main rents with their name and capacity, indexed by their id.
  * @param $subrents List of all subrents with their capacity, indexed by their id.
  * @param $computationYear The year on which the result is computed.
  * @param $splitByMonth Boolean. If true, 'rented_days' is an array of size 12,
  *                      containing the number of rented days times capacity for each month.
  *                      Otherwise, 'rented_days' is an integer containing the same number for the whole year.
  * @return The statistics information computed :
  * {
  *   'rent_id': { 'name': 'rent_name', 'capacity': capacity_number, 'rentedDaysTimesCapacity': rented_days},
  *   'rent_id': ...
  * }
  */
function getRentedDaysByRentStatistics($bdd, $rents, $subrents, $computationYear, $splitByMonth) {
  
    // Initialize rents field
    foreach ($rents as $rentId => $rentInfo) {
      if ($splitByMonth)
        $rents[$rentId]['rentedDaysTimesCapacity'] = array(0,0,0,0,0,0,0,0,0,0,0,0);
      else
        $rents[$rentId]['rentedDaysTimesCapacity'] = 0;
    }
  
    // Get bookings information
    $req = $bdd->prepare('SELECT rent, date, duration FROM booking
                          WHERE YEAR(date) = :year OR YEAR(DATE_ADD(date, INTERVAL duration DAY)) = :year');
    $req->execute(array('year' => $computationYear));
    while ($res = $req->fetch()) {
      $mainRent = "";
      $capacity = 0;
      
      if ($subrents[$res['rent']]) {
        // It is a subrent
        $mainRent = $subrents[$res['rent']]['rent'];
        $capacity = intval($subrents[$res['rent']]['capacity']);
      }
      else {
        // It is not a sub rent
        $mainRent = $res['rent'];
        $capacity = intval($rents[$res['rent']]['capacity']);
      }
      // For each rented days of this booking, add the capacity to the right rent
      $date = new DateTime($res['date']);
      for ($i = 0; $i < $res['duration']; $i++) {
        $year = $date->format('Y');
        if ($year == $computationYear) {
          if ($splitByMonth) {
            $month = intval($date->format('m')) - 1;
            $rents[$mainRent]['rentedDaysTimesCapacity'][$month] += $capacity;
          }
          else {
            $rents[$mainRent]['rentedDaysTimesCapacity'] += $capacity;
          }
        }
        $date->add(new DateInterval('P1D'));
      }
    }
    $req->closeCursor();
    
    return $rents;
}

function readAllFile($filename) {
  $file = fopen($filename, "r+");
  $res = fread($file, filesize($filename));
  fclose($file);
  return $res;
}

function writeFile($filename, $content) {
  $file = fopen($filename, "w");
  fwrite($file, $content);
  fclose($file);
}

?>