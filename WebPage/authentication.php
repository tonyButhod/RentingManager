<?php
if(!isset($_POST['login']) || 
    (!isset($_POST['password']) && !isset($_POST['hash']))) {
  echo "Access denied !";
  exit();
}
include('./bdd.php');

$bdd_user = $bdd->prepare('SELECT * FROM user WHERE login = :login AND password = :password');
if (isset($_POST['password'])) {
  $password = hash("sha512", $_POST['password'], false);
}
else {
  $password = $_POST['hash'];
}
$bdd_user->execute(array('login' => $_POST['login'],
                         'password' => $password));
$user = $bdd_user->fetchAll();
$bdd_user->closeCursor();

if (!isset($user[0])) {
  exit();
}

?>