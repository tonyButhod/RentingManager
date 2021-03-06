<?php
include('authentication.php');

if (!isset($_POST['name'])) {
  exit();
}

// Select subrents of the given rent (and the rent itself)
$req = $bdd->prepare('SELECT * FROM rent
                      WHERE id IN (
                          SELECT s.subrent FROM subrent s, rent r
                          WHERE s.rent = r.id AND r.name = :name
                      ) OR name = :name
                      ORDER BY id;');
$req->execute(array('name' => $_POST['name']));
$subrents = [];
$subrents_id = [];
$idRent = -1;
while ($rent = $req->fetch()) {
  $subrents[] = array('id' => $rent['id'],
                      'name' => $rent['name']);
  $subrents_id[] = $rent['id'];
  if ($_POST['name'] == $rent['name'])
    $idRent = $rent['id'];
}
$req->closeCursor();
// If idRent == -1, the rent name is invalid
if ($idRent == -1) {
  echo "Invalid rent name";
  exit();
}

// Recover information concerning booking
$req = null;
if (isset($_POST['minDate'])) {
  // Select booking after a minimum date to limit the flow of data
  $minDate = new DateTime($_POST['minDate']);
  $req = $bdd->prepare('SELECT * FROM booking
                        WHERE rent IN ('.join(",",$subrents_id).')
                        AND DATE_ADD(date, INTERVAL duration-1 DAY) >= :minDate;');
  $req->execute(array('minDate' => $minDate->format("Y-m-d")));
}
else {
  $req = $bdd->prepare('SELECT * FROM booking
                        WHERE rent IN ('.join(",",$subrents_id).');');
  $req->execute();
}
$booking = [];
while ($res = $req->fetch()) {
  $booking[] = array('id' => $res['id'],
                     'rent' => $res['rent'],
                     'date' => date("Ymd", strtotime($res['date'])),
                     'duration' => $res['duration'],
                     'tenant' => $res['tenant']);
}
$req->closeCursor();

// Select owners of the rent
$req = $bdd->prepare('SELECT u.* FROM user u, owner o
                      WHERE u.id = o.user AND o.rent = :rent;');
$req->execute(array('rent' => $idRent));
$owners = []; // A rent can be owned by several people.
while ($res = $req->fetch()) {
  $owners[] = array('username' => $res['username']);
}
$req->closeCursor();

  
echo json_encode(array('username' => $user['username'],
                       'hash' => $user['password'],
                       'access' => $user['access'],
                       'owners' => $owners,
                       'subrents' => $subrents,
                       'booking' => $booking));
?>