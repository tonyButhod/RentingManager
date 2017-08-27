<?php
if(!isset($_POST['username']) || 
    (!isset($_POST['password']) && !isset($_POST['hash']))) {
  echo "Access denied !";
  exit();
}
include('./bdd.php');

$bdd_user = $bdd->prepare('SELECT * FROM user WHERE username = :username AND password = :password');
if (isset($_POST['password'])) {
  $password = hash("sha512", $_POST['password'], false);
}
else {
  $password = $_POST['hash'];
}
$bdd_user->execute(array('username' => $_POST['username'],
                         'password' => $password));
$user = $bdd_user->fetch();
$bdd_user->closeCursor();

if (!$user) {
  exit();
}

?>