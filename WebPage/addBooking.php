<?php
include('authentication.php');

if (!isset($_POST['rent']) || !isset($_POST['tenant']) || 
    !isset($_POST['date']) || !isset($_POST['duration'])) {
  exit();
}

// Check if the id is in the table of rents and if the table is a sub-rent or not.
$req = $bdd->prepare('SELECT subrent, COUNT(rent) as nb FROM (
                          SELECT rent, subrent FROM subrent
                          WHERE subrent = :rent
                          UNION
                          SELECT id as rent, id as subrent FROM rent
                          WHERE id = :rent
                      ) as temp
                      GROUP BY subrent;');
$req->execute(array('rent' => $_POST['rent']));
$result = $req->fetch();
$req->closeCursor();

$isSubrent = false;
if ($result) {
  // Check if the rent id is a sub-rent or the whole rent
  if ($result['nb'] > 1)
    $isSubrent = true;
}
else {
  // The given rent id is unknown
  echo "Rent id unknown";
  exit();
}

// Check if the user has the right to add a booking
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

$bdd->beginTransaction();
$req = $bdd->prepare('SELECT * FROM booking
                      WHERE rent = :rent OR rent IN (
                          SELECT rent FROM subrent WHERE subrent = :rent
                      );');
$req->execute(array('rent' => $_POST['rent']));
/* Check if the rent is free */
while ($booking = $req->fetch()) {
  $bookingDate = new DateTime($booking['date']);
  $startDate = new DateTime($_POST['date']);
  $endDate = new DateTime($_POST['date']);
  $endDate->add(new DateInterval('P'.($_POST['duration']-1).'D'));
  if ($bookingDate <= $endDate) {
    $bookingDate->add(new DateInterval('P'.($booking['duration']-1).'D'));
    if ($bookingDate >= $startDate) {
      /* The rent is not free */
      echo "Rent not free";
      exit();
    }
  }
}
$req->closeCursor();
/* The rent is free, so the booking is inserted */
$req = $bdd->prepare('INSERT INTO booking (rent, date, duration, tenant)
                      SELECT :rent, :date, :duration, :tenant;');
$req->execute(array('rent' => $_POST['rent'],
                    'date' => DateTime::createFromFormat('Ymd', $_POST['date'])->format('Y-m-d'),
                    'duration' => $_POST['duration'],
                    'tenant' => $_POST['tenant']));
$rowsInserted = $req->rowCount();
$req->closeCursor();
$bdd->commit();

if ($rowsInserted == 1)
  echo "OK";
else
  echo "Rent not free";

?>