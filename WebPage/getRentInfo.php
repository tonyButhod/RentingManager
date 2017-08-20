<?php
include('authentication.php');

if (!isset($_POST['name'])) {
  exit();
}

$req = $bdd->prepare('SELECT * FROM rent
                      WHERE id IN (
                          SELECT s.subrent FROM subrent s, rent r
                          WHERE s.rent = r.id AND r.name = :name
                      );');
$req->execute(array('name' => $_POST['name']));
$subrents = $req->fetchAll();
$req->closeCursor();

  
echo json_encode(array('login' => $user[0]['login'],
                       'password' => $user[0]['password'],
                       'subrents' => $subrents));
?>