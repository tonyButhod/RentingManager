<?php
include('authentication.php');

switch ($_POST['statistics']) {
  case 0: // Get rents per month
  
    // Get sub-rents information
    $req = $bdd->prepare('SELECT rent, subrent FROM subrent;');
    $req->execute();
    $subrents = [];
    while ($res = $req->fetch()) {
      $subrents[$res['subrent']] = array('rent' => $res['rent']);
    }
    $req->closeCursor();
    
    // Get main rents with their names and the number of sub-rents
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
                                    'capacity' => intval($rent['capacity']),
                                    'rentedDaysTimesCapacity' => array(0,0,0,0,0,0,0,0,0,0,0,0));
      }
    }
    $req->closeCursor();
  
    // Get all booking information
    $req = $bdd->prepare('SELECT rent, date, duration FROM booking
                          WHERE YEAR(date) = :year');
    $req->execute(array('year' => $_POST['year']));
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
      
      $date = new DateTime($res['date']);
      for ($i = 0; $i < $res['duration']; $i++) {
        $month = intval($date->format('m')) - 1;
        $year = $date->format('Y');
        if ($year == $_POST['year'])
          $rents[$mainRent]['rentedDaysTimesCapacity'][$month] += $capacity;
        $date->add(new DateInterval('P1D'));
      }
    }
    $req->closeCursor();
    
    // Send statistics results
    echo json_encode($rents);
    break;
}

?>