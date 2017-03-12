namespace DeCry
{
    partial class Form1
    {
        /// <summary>
        /// 必需的设计器变量。
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// 清理所有正在使用的资源。
        /// </summary>
        /// <param name="disposing">如果应释放托管资源，为 true；否则为 false。</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows 窗体设计器生成的代码

        /// <summary>
        /// 设计器支持所需的方法 - 不要修改
        /// 使用代码编辑器修改此方法的内容。
        /// </summary>
        private void InitializeComponent()
        {
            this.srcFilePathText = new System.Windows.Forms.TextBox();
            this.openFileButton = new System.Windows.Forms.Button();
            this.saveAsButton = new System.Windows.Forms.Button();
            this.saveAsFilePathText = new System.Windows.Forms.TextBox();
            this.passwdText = new System.Windows.Forms.TextBox();
            this.begainButton = new System.Windows.Forms.Button();
            this.statusLable = new System.Windows.Forms.Label();
            this.statusProgress = new System.Windows.Forms.ProgressBar();
            this.label1 = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // srcFilePathText
            // 
            this.srcFilePathText.Location = new System.Drawing.Point(12, 26);
            this.srcFilePathText.Name = "srcFilePathText";
            this.srcFilePathText.Size = new System.Drawing.Size(651, 25);
            this.srcFilePathText.TabIndex = 0;
            // 
            // openFileButton
            // 
            this.openFileButton.Location = new System.Drawing.Point(695, 19);
            this.openFileButton.Name = "openFileButton";
            this.openFileButton.Size = new System.Drawing.Size(121, 34);
            this.openFileButton.TabIndex = 1;
            this.openFileButton.Text = "选择源文件";
            this.openFileButton.UseVisualStyleBackColor = true;
            this.openFileButton.Click += new System.EventHandler(this.openFileButton_Click);
            // 
            // saveAsButton
            // 
            this.saveAsButton.Location = new System.Drawing.Point(695, 85);
            this.saveAsButton.Name = "saveAsButton";
            this.saveAsButton.Size = new System.Drawing.Size(121, 33);
            this.saveAsButton.TabIndex = 3;
            this.saveAsButton.Text = "保存为";
            this.saveAsButton.UseVisualStyleBackColor = true;
            this.saveAsButton.Click += new System.EventHandler(this.saveAsButton_Click);
            // 
            // saveAsFilePathText
            // 
            this.saveAsFilePathText.Location = new System.Drawing.Point(12, 83);
            this.saveAsFilePathText.Name = "saveAsFilePathText";
            this.saveAsFilePathText.Size = new System.Drawing.Size(651, 25);
            this.saveAsFilePathText.TabIndex = 2;
            // 
            // passwdText
            // 
            this.passwdText.Location = new System.Drawing.Point(122, 220);
            this.passwdText.Name = "passwdText";
            this.passwdText.Size = new System.Drawing.Size(541, 25);
            this.passwdText.TabIndex = 4;
            // 
            // begainButton
            // 
            this.begainButton.Location = new System.Drawing.Point(695, 220);
            this.begainButton.Name = "begainButton";
            this.begainButton.Size = new System.Drawing.Size(121, 25);
            this.begainButton.TabIndex = 5;
            this.begainButton.Text = "开始解密";
            this.begainButton.UseVisualStyleBackColor = true;
            this.begainButton.Click += new System.EventHandler(this.begainButton_Click);
            // 
            // statusLable
            // 
            this.statusLable.AutoSize = true;
            this.statusLable.ForeColor = System.Drawing.SystemColors.AppWorkspace;
            this.statusLable.Location = new System.Drawing.Point(708, 287);
            this.statusLable.Name = "statusLable";
            this.statusLable.Size = new System.Drawing.Size(23, 15);
            this.statusLable.TabIndex = 6;
            this.statusLable.Text = "0%";
            // 
            // statusProgress
            // 
            this.statusProgress.Location = new System.Drawing.Point(12, 287);
            this.statusProgress.Name = "statusProgress";
            this.statusProgress.Size = new System.Drawing.Size(651, 23);
            this.statusProgress.TabIndex = 7;
            // 
            // label1
            // 
            this.label1.Location = new System.Drawing.Point(25, 225);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(70, 25);
            this.label1.TabIndex = 8;
            this.label1.Text = "输入密码";
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 15F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(843, 384);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.statusProgress);
            this.Controls.Add(this.statusLable);
            this.Controls.Add(this.begainButton);
            this.Controls.Add(this.passwdText);
            this.Controls.Add(this.saveAsButton);
            this.Controls.Add(this.saveAsFilePathText);
            this.Controls.Add(this.openFileButton);
            this.Controls.Add(this.srcFilePathText);
            this.Name = "Form1";
            this.Text = "YY1818解密软件示例";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox srcFilePathText;
        private System.Windows.Forms.Button openFileButton;
        private System.Windows.Forms.Button saveAsButton;
        private System.Windows.Forms.TextBox saveAsFilePathText;
        private System.Windows.Forms.TextBox passwdText;
        private System.Windows.Forms.Button begainButton;
        private System.Windows.Forms.Label statusLable;
        private System.Windows.Forms.ProgressBar statusProgress;
        private System.Windows.Forms.Label label1;
    }
}

