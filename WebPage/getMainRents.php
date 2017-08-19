<?php
include('authentication.php');

$req = $bdd->prepare('SELECT * FROM rent
                      Where id NOT IN (
                          SELECT DISTINCT subrent FROM subrent
                      );');
$req->execute();
$mainRents = $req->fetchAll();
$req->closeCursor();

  
echo json_encode(array('login' => $user[0]['login'],
                       'password' => $user[0]['password'],
                       'rents' => $mainRents));
?>