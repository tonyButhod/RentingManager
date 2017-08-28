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
$subrents_id = [];
$subrents_names = [];
$subrents_weeks = [];
$subrents_prices = [];
$idRent = -1;
while ($rent = $req->fetch()) {
  $subrents_id[] = $rent['id'];
  $subrents_names[] = $rent['name'];
  $subrents_weeks[$rent['id']] = [];
  $subrents_prices[$rent['id']] = [];
  if ($_POST['name'] == $rent['name'])
    $idRent = $rent['id'];
}
$req->closeCursor();
// If idRent == -1, the rent name is invalid
if ($idRent == -1) {
  echo "Invalid rent name";
  exit();
}

// Recover information concerning prices
$req = $bdd->prepare('SELECT * FROM price
                      WHERE rent IN ('.join(",",$subrents_id).');');
$req->execute();
while ($res = $req->fetch()) {
  $subrents_weeks[$res['rent']][] = $res['week'];
  $subrents_prices[$res['rent']][] = $res['price'];
}
$req->closeCursor();

$prices = [];
for ($i = 0; $i < count($subrents_id); $i++) {
  $rent_id = $subrents_id[$i];
  $rent_name = $subrents_names[$i];
  $prices[] = array('id' => $rent_id,
                    'name' => $rent_name,
                    'weeks' => $subrents_weeks[$rent_id],
                    'prices' => $subrents_prices[$rent_id]);
}
  
echo json_encode(array('username' => $user['username'],
                       'hash' => $user['password'],
                       'prices' => $prices));
?>