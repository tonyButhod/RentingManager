<?php

if (!isset($_POST['username']) || !isset($_POST['password']) || !isset($_POST['newPassword'])) {
  exit();
}

include('bdd.php');

$bdd_user = $bdd->prepare('SELECT * FROM user WHERE username = :username AND password = :password');
$bdd_user->execute(array('username' => $_POST['username'],
                         'password' => hash("sha512", $_POST['password'], false)));
$user = $bdd_user->fetch();
$bdd_user->closeCursor();

if (!$user) {
  echo "Password incorrect";
  exit();
}
if (strlen($_POST['newPassword']) < 8) {
  echo "At least 8 characters";
  exit();
}
// The given password is correct, the new one is updated
$newPassword = hash("sha512", $_POST['newPassword'], false);
$req = $bdd->prepare('UPDATE user 
                      SET password = :newPassword 
                      WHERE username = :username');
$req->execute(array('username' => $_POST['username'],
                    'newPassword' => $newPassword));
$req->closeCursor();

// Display the new hash of the password
echo $newPassword;

?>