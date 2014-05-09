<html>
<body>
<ul>
<?php
if ($handle = opendir('.')) {
    while (false !== ($file = readdir($handle))) {
        if ($file != "." && $file != ".." && $file != "index.php") {
            echo "<li><a href=\"$file\">$file</a>\n";
        }
    }
    closedir($handle);
}
?>
</ul>
</body>
</html>