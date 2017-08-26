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
while ($rent = $req->fetch()) {
  $subrents[] = array('id' => $rent['id'],
                      'name' => $rent['name']);
  $subrents_id[] = $rent['id'];
}
$req->closeCursor();

// Recover information concerning booking
$req = $bdd->prepare('SELECT * FROM booking
                      WHERE rent IN ('.join(",",$subrents_id).');');
$req->execute();
$booking = [];
while ($res = $req->fetch()) {
  $booking[] = array('rent' => $res['rent'],
                     'week' => $res['week'],
                     'year' => $res['year'],
                     'tenant' => $res['tenant']);
}
$req->closeCursor();

  
echo json_encode(array('login' => $user[0]['login'],
                       'hash' => $user[0]['password'],
                       'subrents' => $subrents,
                       'booking' => $booking));
?>