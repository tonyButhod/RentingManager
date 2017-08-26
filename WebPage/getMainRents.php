<?php
include('authentication.php');

$req = $bdd->prepare('SELECT * FROM rent
                      WHERE id NOT IN (
                          SELECT DISTINCT subrent FROM subrent
                      )
                      ORDER BY id;');
$req->execute();
$mainRents = [];
while ($rent = $req->fetch()) {
  $mainRents[] = array('name'=> $rent['name']);
}
$req->closeCursor();

  
echo json_encode(array('login' => $user[0]['login'],
                       'hash' => $user[0]['password'],
                       'rents' => $mainRents));
?>