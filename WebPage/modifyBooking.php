<?php
include('authentication.php');

if (!isset($_POST['booking']) || 
    !isset($_POST['tenant'])) {
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

// Modify the booking
$req = $bdd->prepare('UPDATE booking
                      SET tenant = :tenant
                      WHERE id = :bookingId;');
$success = $req->execute(array('bookingId' => $_POST['booking'],
                               'tenant' => $_POST['tenant']));
$rowsModified = $req->rowCount();
$req->closeCursor();

if ($success && $rowsModified > 0)
  echo "OK";
else
  echo "Booking doesn't exist";

?>