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

// Check if a new message is stored
$req = $bdd->prepare('SELECT m.message FROM message m, user u
                      WHERE username = :username AND u.message = 1 AND m.id = 1;');
$req->execute(array('username' => $user['username']));
$message = $req->fetch();
$req->closeCursor();
 
$resultArray = array('username' => $user['username'],
                     'hash' => $user['password'],
                     'rents' => $mainRents);
if ($message) {
  $resultArray['message'] = $message['message'];
}
echo json_encode($resultArray);
?>