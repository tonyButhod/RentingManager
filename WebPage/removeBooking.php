<?php
include('authentication.php');

if (!isset($_POST['rent']) || 
    !isset($_POST['week']) || 
    !isset($_POST['year'])) {
  exit();
}

// Check if the user has the right to remove a booking
$req = $bdd->prepare('SELECT DISTINCT o.user FROM owner o, subrent s
                      WHERE o.rent = :rent
                      OR (o.rent = s.rent AND s.subrent = :rent);');
$req->execute(array('rent' => $_POST['rent']));
$owners = [];
while ($res = $req->fetch())
  $owners[] = $res['user'];
$req->closeCursor();

if ($user['access'] == 0 && !in_array($user['id'], $owners)) {
  echo "No booking right";
  exit();
}

// Remove the booking
$req = $bdd->prepare('DELETE FROM booking
                      WHERE week = :week AND year = :year AND rent = :rent;');
$success = $req->execute(array('rent' => $_POST['rent'],
                               'week' => $_POST['week'],
                               'year' => $_POST['year']));
$rowsDeleted = $req->rowCount();
$req->closeCursor();

if ($success && $rowsDeleted > 0)
  echo "true";
else
  echo "Booking doesn't exists";

?>