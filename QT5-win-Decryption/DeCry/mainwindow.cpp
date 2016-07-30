#include "mainwindow.h"
#include "ui_mainwindow.h"
#include<stdio.h>
#include<stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <QCryptographicHash>
#include <QtDebug>
#include <QFile>
#include <QFileDialog>

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    memset(buffer_src_file, 0, 256);
    memset(buffer_src_file, 0, 256);
    connect(ui->openFile, SIGNAL(pressed()), this, SLOT(openFile()));
    connect(ui->begin, SIGNAL(pressed()), this, SLOT(begin()));
    connect(ui->saveAsButton, SIGNAL(pressed()), this, SLOT(chooseDestFile()));
}

MainWindow::~MainWindow()
{
    delete ui;
}

QByteArray MainWindow::passwd2sha512(const char *str)
{
    QCryptographicHash * hash = new QCryptographicHash(QCryptographicHash::Sha512);
    hash->addData(str);
    QByteArray result = hash->result();

        for (int i = 0; i < result.length(); i++)
        {
            qDebug("%d", result.at(i));
        }

    return result;
}

void MainWindow::decryptionFile(const char *src, const char *dest, const char *key, int keylen)
{
    QFile file_src(src);
    file_src.open(QIODevice::ReadOnly);

    QFile file_dest(dest);
    if (file_dest.exists())
    {
        file_dest.remove();
    }
    file_dest.open(QIODevice::ReadWrite);

    ui->progressBar->setMaximum(file_src.size());
    char buffer[1024] = {};
    char buffer_decode[1024] = {};
    for (int i = 0; i < 1024; i++)
    {
        buffer_decode[i] = 111;
//        buffer_decode[i] = key[i % keylen];
    }

    int len = 0;
    int j = 0;
    int cap = file_src.size()  / 1024 / 100; //100 times to update progressbar. otherwise big
    cap = (cap == 0 ? 2 : cap);
    //files make this app scrash.
    int k  = 0;
    qDebug("the cap is %d", cap);
    while (!file_src.atEnd())
    {
        len =  file_src.read(buffer, keylen);
        for (j = 0; j < len; j++){
            buffer[j] -= buffer_decode[j];
        }
        file_dest.write(buffer, len);

        if (k++ % cap == 1){
            //qDebug("the pos is %d", file_dest.pos());
            ui->progressBar->setValue(file_dest.pos());
        }
    }

    file_dest.flush();
    ui->progressBar->setValue(file_dest.pos());
    file_src.close();
    file_dest.close();

}

void MainWindow::openFile()
{
    memset(buffer_src_file, 0, 256);
    QString filename_src = QFileDialog::getOpenFileName(this,
                                                        "choose the src file", "",    " *.*");
    memcpy(buffer_src_file, filename_src.toLatin1().data(), filename_src.size());
    ui->src_file_lable->setText(filename_src);
}

void MainWindow::chooseDestFile()
{
    memset(buffer_dest_file, 0, 256);
    QString filename_dest = QFileDialog::getSaveFileName(this,
                                                         "choose the dest file", "",    " *.*");
    memcpy(buffer_dest_file, filename_dest.toLatin1().data(), filename_dest.size());
    ui->saveas_lable->setText(filename_dest);
}

void MainWindow::begin()
{
    qDebug("begin decode");
    QString passwd = ui->passwd->text();
    if (passwd.length() == 0 || strlen(buffer_dest_file) == 0 || strlen(buffer_src_file) == 0)
    {
        qDebug("no passwd");
        return;
    }
    QByteArray sha523 = passwd2sha512(passwd.toLatin1().data());
    const char * key = sha523.data();
    decryptionFile(buffer_src_file, buffer_dest_file, key, 64);
}

































