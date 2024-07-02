1. 먼저 File Sending & Sharing Service의 기능을 이해하기 위해서 ‘https://send-anywhere.com’ 
사이트를 방문하여, 어떻게 파일 공유 서비스가 제공되고 있는지를 확인해 봅니다. 이곳은 별도의
로그인 과정없이 짧은 광고를 시청한 후에 바로 파일을 업로드할 수 있도록 하였으며, 업로드를
통해 배정받은 6자리의 숫자(pass_code)를 통해 누구든 짧은 광고 시청 후에 파일을 다운받을 수
있도록 서비스하고 있습니다.

이번 프로젝트에서 여러분은 유사한 방식의 서비스를 광고시청 없이 제공하되, 여기에 추가적인
기능과 제한사항을 함께 구현하고자 합니다. 구현할 서비스에서 주된 기능은 사용자가 5분 동안
최대 1개의 파일만을 업로드할 수 있으며, 업로드 된 파일은 최대 5분 동안만 유지되고, 5분
이내에 누군가가 다운로드를 받거나, 5분이 초과될 경우에는 발급된 6자리의 숫자가 무효화되어
더 이상 파일을 다운로드할 수 없게 되는 것입니다.

2. File Sending & Sharing Service는 root 사용자 이외에는 사용자 계정을 관리하지 않으므로,
별도의 사용자 계정 테이블이 필요하지는 않습니다. 하지만, 업로드 된 파일의 pass_code와
서비스 만료 시간 등의 정보를 관리하기 위해서 테이블을 활용하는 것이 훨씬 효과적이기 때문에,
별도의 데이터베이스와 테이블을 생성하여 사용하기로 하겠습니다. 다음의 SQL 명령어를 팀원이
함께 작업하고 있는 MariaDB에 차례대로 실행하여, fileservice database와 filelist table을
준비하도록 합니다.

CREATE DATABASE fileservice;
USE fileservice;
CREATE OR REPLACE TABLE filelist (
 id INT NOT NULL AUTO_INCREMENT,
 pass_code CHAR(6) NULL,
 filename VARCHAR(100) NOT NULL,
 ip_addr VARCHAR(15) NULL,
 upload_time DATETIME NULL,
 PRIMARY KEY (id)
);

INSERT INTO filelist (pass_code, filename, ip_addr, upload_time)
 VALUES ('123456', 'test_user_file.docx', '192.168.1.1', NOW());
SELECT * FROM filelist WHERE NOW() <= DATE_ADD(upload_time, INTERVAL 5 MINUTE);

3. 앞서 실행한 마지막 SELECT SQL 문장을 통해 filelist 테이블에 삽입된 최근 5분 이내의 데이터를
모두 출력해 보면, pass_code의 값이 ‘123456’인 레코드가 하나 삽입되어 있으며, 여기에
기본키가 되는 id의 값이 1로 자동적으로 할당(AUTO_INCREMENT)되어 있는 것을 확인할 수
있습니다. 위의 4번과 5번의 SQL 명령에서 사용된 NOW() 함수는 현재의 시각을 반환하며, 5번의
SELECT SQL에서 사용된 DATE_ADD(…) 함수는 DATETIME 데이터형에 날짜나 시간 등을
더하거나 빼는 연산을 수행하는 것으로, 위의 5번 명령에서는 upload_time에 5분을 더한 값이
현재 시간보다 큰 레코드를 모두 출력하는데 사용되고 있습니다.
- 참고로 filelist 테이블의 pass_code는 파일을 업로드한 사용자에게 발급되는 6자리의 무작위
숫자이며, 기본키가 아니기 때문에 테이블 내에서 값이 중복되더라도 문제가 되지는 않습니다.
하지만 파일이 업로드 된 후부터 최대 5분 동안에는 다른 파일의 pass_code와 중복되지
않도록 웹 어플리케이션 차원에서 적절히 구분하여 발급해 줄 수 있어야 합니다.

4. 실습을 통해 사용했던 여러 예제 파일들을 참고하여 File Sending & Sharing Service의
사용자 UI를 구성하도록 하고, 추가적인 UI가 필요하다면 팀 내에서 자율적으로 결정하여
사용하도록 합니다. 가급적 servlet class와 추가적인 JAVA class들의 패키지는 ‘site.manage’로
통일하여 사용하도록 합니다.
- UI의 디자인이 화려할 필요는 없으며, 사용성에 문제가 없다면 UI 디자인은 실제 평가에 영향을
미치지 않기 때문에 외관보다는 기능에 충실한 구현이 되도록 합니다.

